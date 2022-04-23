/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import me.philcali.device.pool.iot.HostExpansionIoT;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.iot.IotClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

@Singleton
public class ObtainDeviceFunction implements Function<ObtainDeviceRequest, ObtainDeviceResponse> {
    private static final int MAX_LEASES = 1;
    private static final Logger LOGGER = LogManager.getLogger(ObtainDeviceFunction.class);
    private final LockRepo lockRepo;
    private final ProvisionRepo provisionRepo;
    private final DevicePoolRepo devicePoolRepo;
    private final IotClient iot;
    private final Configuration configuration;


    @Inject
    public ObtainDeviceFunction(
            IotClient iot,
            Configuration configuration,
            LockRepoDynamo lockRepo,
            ProvisionRepoDynamo provisionRepo,
            DevicePoolRepoDynamo devicePoolRepo) {
        this.iot = iot;
        this.configuration = configuration;
        this.lockRepo = lockRepo;
        this.provisionRepo = provisionRepo;
        this.devicePoolRepo = devicePoolRepo;
    }

    private String previousToken(CompositeKey provisionKey) {
        String nextToken = null;
        try {
            nextToken = Optional.ofNullable(lockRepo.get(provisionKey).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(null);
        } catch (NotFoundException nfe) {
            LOGGER.info("No lock found for {}, rotating", provisionKey);
        }
        return nextToken;
    }

    private ExpandingHostProvider.ExpansionFunction createHostExpansion(String poolId) {
        return HostExpansionIoT.builder()
                .iot(iot)
                .thingGroup(poolId)
                .recursive(configuration.recursive())
                .build();
    }

    @Override
    public ObtainDeviceResponse apply(ObtainDeviceRequest request) {
        CompositeKey poolKey = devicePoolRepo.resourceKey(request.accountKey(), request.provision().poolId());
        CompositeKey provisionKey = provisionRepo.resourceKey(poolKey, request.provision().id());
        ExpandingHostProvider.NextSetHosts nextSet = createHostExpansion(request.provision().poolId())
                .apply(previousToken(provisionKey), MAX_LEASES);
        LockObject lock = lockRepo.extend(provisionKey, CreateLockObject.builder()
                .holder(request.provision().poolId())
                .duration(Duration.ofHours(1))
                .value(Optional.ofNullable(nextSet.nextToken()).orElse(""))
                .build());
        LOGGER.info("Extended held lock for {} for {}, expires in {}",
                provisionKey, request.provision().poolId(), lock.expiresIn());
        return nextSet.hosts().stream().findFirst().map(device -> ObtainDeviceResponse.builder()
                .status(Status.SUCCEEDED)
                .device(DeviceObject.builder()
                        .id(device.deviceId())
                        .publicAddress(device.hostName())
                        .expiresIn(request.provision().expiresIn())
                        .poolId(request.provision().poolId())
                        .updatedAt(Instant.now())
                        .build())
                .accountKey(request.accountKey())
                .build())
                .orElseThrow(() -> new IllegalStateException("Could not find an appropriate AWS IoT thing"));
    }
}
