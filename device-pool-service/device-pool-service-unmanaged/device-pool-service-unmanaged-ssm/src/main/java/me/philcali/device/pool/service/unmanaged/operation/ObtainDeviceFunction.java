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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
        Optional<String> deviceId = previouslyObtainedDevice(provisionKey(request));
        Comparator<InstanceInformation> byInstanceId = Comparator.comparing(InstanceInformation::instanceId);
        AtomicReference<InstanceInformation> firstInstance = new AtomicReference<>();
        Optional<InstanceInformation> firstAvailable = ssm.describeInstanceInformationPaginator(DescribeInstanceInformationRequest.builder()
                        .filters(fb -> fb.key("tag:DevicePool").values(request.provision().poolId()))
                        .build())
                .instanceInformationList()
                .stream()
                // Only want online instances
                .filter(instance -> instance.pingStatus().equals(PingStatus.ONLINE))
                .peek(instance -> firstInstance.accumulateAndGet(instance,
                        (left, right) -> left == null || byInstanceId.compare(left, right) > 0 ? right : left))
                .filter(instance -> deviceId.isEmpty() || deviceId.get().compareTo(instance.instanceId()) < 0)
                .findFirst();
        // The first available instance, may not be present for a couple of reasons
        // 1. There are no instances... in this case the first instance will be null
        // 2. There are no instances "greater" than the last one... here we cycle to first
        Optional<InstanceInformation> usableInstance = firstAvailable.isPresent() ?
                firstAvailable :
                Optional.ofNullable(firstInstance.get());
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

    private Optional<String> previouslyObtainedDevice(CompositeKey provisionKey) {
        try {
            return Optional.ofNullable(lockRepo.get(provisionKey).value());
        } catch (NotFoundException nfe) {
            LOGGER.info("No previously obtained device for provision {}", provisionKey);
            return Optional.empty();
        }
    }

    private ObtainDeviceResponse lockAndTransform(ObtainDeviceRequest request, DeviceObject device) {
        LockObject updatedLock = lockRepo.extend(provisionKey(request), CreateLockObject.builder()
                .value(device.id())
                .holder(device.poolId())
                .duration(Duration.ofHours(1))
                .build());
        LOGGER.info("Updated provision lock with {} for {}", updatedLock.value(), request.provision().id());
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
                        .map(device -> lockAndTransform(request, device))
                        .orElseThrow(() -> new IllegalStateException("Could not find an applicable device"));
            default:
                throw new IllegalArgumentException("Provision strategy of "
                        + configuration.provisionStrategy() + " is not recognized");
        }
    }
}
