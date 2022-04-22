/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Singleton
public class ObtainDeviceFunction implements OperationFunction<ObtainDeviceRequest, ObtainDeviceResponse> {
    private static final int MAX_LEASES = 1;
    private static final Logger LOGGER = LogManager.getLogger(ObtainDeviceFunction.class);
    private final Configuration configuration;
    private final LockRepo lockRepo;
    private final ProvisionRepo provisionRepo;
    private final DevicePoolRepo devicePoolRepo;

    private final ExpandingHostProvider.ExpansionFunction hosts;

    @Inject
    public ObtainDeviceFunction(
            ExpandingHostProvider.ExpansionFunction hosts,
            Configuration configuration,
            LockRepoDynamo lockRepo,
            ProvisionRepoDynamo provisionRepo,
            DevicePoolRepoDynamo devicePoolRepo) {
        this.hosts = hosts;
        this.configuration = configuration;
        this.lockRepo = lockRepo;
        this.provisionRepo = provisionRepo;
        this.devicePoolRepo = devicePoolRepo;
    }

    private void acquireLockOrFail(ObtainDeviceRequest request) {
        if (configuration.locking()) {
            CompositeKey poolKey = devicePoolRepo.resourceKey(request.accountKey(), request.provision().poolId());

            LockObject lockObject = lockRepo.extend(poolKey, CreateLockObject.builder()
                    .duration(configuration.lockingDuration())
                    .holder(provisionRepo.resourceKey(poolKey, request.provision().id()).toString())
                    .value(configuration.thingGroup())
                    .build());
            LOGGER.info("Successfully locked pool {} for {}, expires {}",
                    request.provision().poolId(),
                    request.provision().id(),
                    lockObject.expiresIn());
        }
    }

    private String previousToken(CompositeKey provisionKey) {
        String nextToken = null;
        try {
            nextToken = Optional.ofNullable(lockRepo.get(provisionKey).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(null);
        } catch (NotFoundException nfe) {
            LOGGER.info("No lock found, rotating");
        }
        return nextToken;
    }

    @Override
    public ObtainDeviceResponse apply(ObtainDeviceRequest request) {
        acquireLockOrFail(request);
        CompositeKey poolKey = devicePoolRepo.resourceKey(request.accountKey(), request.provision().poolId());
        CompositeKey provisionKey = provisionRepo.resourceKey(poolKey, request.provision().id());
        ExpandingHostProvider.NextSetHosts nextSet = hosts.apply(previousToken(provisionKey), MAX_LEASES);
        LockObject lock = lockRepo.extend(provisionKey, CreateLockObject.builder()
                .holder(request.provision().poolId())
                .duration(Duration.ofHours(1))
                .value(Optional.ofNullable(nextSet.nextToken()).orElse(""))
                .build());
        LOGGER.info("Extended held lock for {} for {}, expires at {}",
                provisionKey, request.provision().poolId(), lock.expiresIn());
        return nextSet.hosts().stream().findFirst().map(device -> ObtainDeviceResponse.builder()
                .device(DeviceObject.builder()
                        .id(device.deviceId())
                        .publicAddress(device.hostName())
                        .expiresIn(request.provision().expiresIn())
                        .poolId(request.provision().poolId())
                        .updatedAt(Instant.now())
                        .build())
                .accountKey(request.accountKey())
                .build())
                .orElseThrow(() -> new IllegalStateException("Could not find an appropriate IoT device"));
    }

    @Override
    public Class<ObtainDeviceRequest> inputType() {
        return ObtainDeviceRequest.class;
    }
}
