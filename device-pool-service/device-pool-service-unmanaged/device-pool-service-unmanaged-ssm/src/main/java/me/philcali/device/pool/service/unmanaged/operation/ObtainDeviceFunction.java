/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.LockRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import me.philcali.device.pool.service.unmanaged.Configuration;
import me.philcali.device.pool.service.unmanaged.PaginationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class ObtainDeviceFunction implements OperationFunction<ObtainDeviceRequest, ObtainDeviceResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObtainDeviceFunction.class);
    private final SsmClient ssm;
    private final LockRepo lockRepo;
    private final DevicePoolRepo poolRepo;
    private final ProvisionRepo provisions;
    private final Configuration configuration;

    @Inject
    public ObtainDeviceFunction(
            final SsmClient ssm,
            final LockRepoDynamo lockRepo,
            final ProvisionRepoDynamo provisions,
            final DevicePoolRepoDynamo poolRepo,
            final Configuration configuration) {
        this.lockRepo = lockRepo;
        this.ssm = ssm;
        this.provisions = provisions;
        this.poolRepo = poolRepo;
        this.configuration = configuration;
    }

    @Override
    public Class<ObtainDeviceRequest> inputType() {
        return ObtainDeviceRequest.class;
    }

    public Optional<DeviceObject> cycleToNext(ObtainDeviceRequest request) {
        CompositeKey provisionKey = provisionKey(request);
        Optional<PaginationToken> startingToken = previousStartingToken(provisionKey);
        DescribeInstanceInformationRequest.Builder builder = DescribeInstanceInformationRequest.builder()
                .maxResults(PaginationToken.MAX_ITEMS)
                .filters(fb -> fb.key("tag:DevicePool").values(request.provision().poolId()));
        startingToken.map(PaginationToken::nextToken).ifPresent(builder::nextToken);
        DescribeInstanceInformationResponse response = ssm.describeInstanceInformation(builder.build());
        List<InstanceInformation> instances = response.instanceInformationList();
        PaginationToken currentToken = startingToken.orElseGet(PaginationToken::create);
        InstanceInformation firstInstance = null;
        if (!instances.isEmpty()) {
            try {
                firstInstance = instances.get(currentToken.index());
                currentToken = currentToken.nextPage(response.nextToken());
                if (Objects.isNull(response.nextToken())) {
                    // Trigger out of bounds to cycle to first
                    instances.get(currentToken.index());
                }
                LockObject updatedLock = lockRepo.extend(provisionKey, CreateLockObject.builder()
                        .duration(Duration.ofHours(1))
                        .holder(request.provision().poolId())
                        .value(currentToken.toString())
                        .build());
                LOGGER.info("Updated provision lock with {} for {}", updatedLock.value(), request.provision().id());
            } catch (IndexOutOfBoundsException e) {
                // We need to rotate. Flush cache for first item
                // TODO: add delete item support
                lockRepo.extend(provisionKey, CreateLockObject.builder()
                        .duration(Duration.ofHours(1))
                        .holder(request.provision().poolId())
                        .value(PaginationToken.create().toString())
                        .build());
                LOGGER.info("Expired paging value for {}", request.provision().id());
            }
        }
        Optional<InstanceInformation> usableInstance = Optional.ofNullable(firstInstance)
                .filter(instance -> instance.pingStatus().equals(PingStatus.ONLINE));
        return usableInstance.map(instance -> DeviceObject.builder()
                .id(instance.instanceId())
                .publicAddress(instance.ipAddress())
                .poolId(request.provision().poolId())
                .updatedAt(instance.lastPingDateTime())
                .expiresIn(request.provision().expiresIn())
                .build());
    }

    private CompositeKey provisionKey(ObtainDeviceRequest request) {
        return provisions.resourceKey(request.accountKey(), request.provision().id());
    }

    private void acquirePoolLockOrFail(final ObtainDeviceRequest request) {
        if (configuration.locking()) {
            CompositeKey poolKey = poolRepo.resourceKey(request.accountKey(), request.provision().poolId());
            // Before obtaining any device from SSM, we'll check if there's an active lock on the pool
            // We want to avoid conflicting device acquisition between provision requests
            LockObject lockObject = lockRepo.extend(poolKey, CreateLockObject.builder()
                    .duration(configuration.lockingDuration())
                    .holder(provisionKey(request).toString())
                    .build());
            LOGGER.info("Obtained a lock for provision {}, expires in {}",
                    request.provision().id(),
                    lockObject.expiresIn());
        }
    }

    private Optional<PaginationToken> previousStartingToken(CompositeKey provisionKey) {
        try {
            return Optional.ofNullable(lockRepo.get(provisionKey).value())
                    .map(PaginationToken::fromString);
        } catch (NotFoundException nfe) {
            LOGGER.info("No previously obtained starting token for provision {}", provisionKey);
            return Optional.empty();
        }
    }

    private ObtainDeviceResponse transformResult(ObtainDeviceRequest request, DeviceObject device) {
        return ObtainDeviceResponse.builder()
                .device(device)
                .status(Status.SUCCEEDED)
                .accountKey(request.accountKey())
                .build();
    }

    @Override
    public ObtainDeviceResponse apply(final ObtainDeviceRequest request) {
        acquirePoolLockOrFail(request);
        switch (configuration.provisionStrategy()) {
            case CYCLIC:
                return cycleToNext(request)
                        .map(device -> transformResult(request, device))
                        .orElseThrow(() -> new IllegalStateException("Could not find an applicable device"));
            default:
                throw new IllegalArgumentException("Provision strategy of "
                        + configuration.provisionStrategy() + " is not recognized");
        }
    }
}
