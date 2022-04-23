/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ExpandingHostProvider;
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
import me.philcali.device.pool.ssm.HostExpansionSSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Singleton
public class ObtainDeviceFunction implements Function<ObtainDeviceRequest, ObtainDeviceResponse> {
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
        this.poolRepo = poolRepo;
        this.provisions = provisions;
        this.configuration = configuration;
    }

    private ExpandingHostProvider.ExpansionFunction createHostFunction(String poolId) {
        return HostExpansionSSM.builder()
                .ssm(ssm)
                .poolId(poolId)
                .architecture("Unknown")
                .build();
    }

    public Optional<DeviceObject> cycleToNext(ObtainDeviceRequest request) {
        CompositeKey provisionKey = provisionKey(request);
        Optional<PaginationToken> startingToken = previousStartingToken(provisionKey);
        ExpandingHostProvider.ExpansionFunction hosts = createHostFunction(request.provision().poolId());
        PaginationToken currentToken = startingToken.orElseGet(PaginationToken::create);
        ExpandingHostProvider.NextSetHosts nextPage = hosts.apply(currentToken.nextToken(), PaginationToken.MAX_ITEMS);
        Host firstInstance = null;
        if (!nextPage.hosts().isEmpty()) {
            try {
                firstInstance = nextPage.hosts().get(currentToken.index());
                currentToken = currentToken.nextPage(nextPage.nextToken());
                if (Objects.isNull(nextPage.nextToken())) {
                    // Trigger out of bounds to cycle to first
                    nextPage.hosts().get(currentToken.index());
                }
            } catch (IndexOutOfBoundsException e) {
                // We need to rotate. Flush cache for first item
                // TODO: add delete item support
                currentToken = PaginationToken.create();
                LOGGER.info("Expired paging value for {}", request.provision().id());
            }
        }
        LockObject updatedLock = lockRepo.extend(provisionKey, CreateLockObject.builder()
                .duration(Duration.ofHours(1))
                .holder(request.provision().poolId())
                .value(currentToken.toString())
                .build());
        LOGGER.info("Updated provision lock with {} for {}, expires in {}",
                updatedLock.value(), request.provision().id(), updatedLock.expiresIn());
        return Optional.ofNullable(firstInstance).map(instance -> DeviceObject.builder()
                .id(instance.deviceId())
                .publicAddress(instance.hostName())
                .poolId(request.provision().poolId())
                .updatedAt(Instant.now())
                .expiresIn(request.provision().expiresIn())
                .build());
    }

    private CompositeKey provisionKey(ObtainDeviceRequest request) {
        CompositeKey poolKey = poolRepo.resourceKey(request.accountKey(), request.provision().poolId());
        return provisions.resourceKey(poolKey, request.provision().id());
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
