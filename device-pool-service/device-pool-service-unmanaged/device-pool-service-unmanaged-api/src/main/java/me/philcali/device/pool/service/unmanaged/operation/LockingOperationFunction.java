/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.LockRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.rpc.model.AccountRequest;
import me.philcali.device.pool.service.unmanaged.model.LockingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Function;

@Singleton
public class LockingOperationFunction<I extends AccountRequest> implements Function<I, I> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LockingOperationFunction.class);
    private final DevicePoolRepo poolRepo;
    private final ProvisionRepo provisionRepo;
    private final LockRepo lockRepo;
    private final LockingConfiguration configuration;

    @Inject
    public LockingOperationFunction(
            final DevicePoolRepoDynamo poolRepo,
            final ProvisionRepoDynamo provisionRepo,
            final LockRepoDynamo lockRepo,
            final LockingConfiguration configuration) {
        this.poolRepo = poolRepo;
        this.provisionRepo =provisionRepo;
        this.lockRepo = lockRepo;
        this.configuration = configuration;
    }

    @Override
    public I apply(I input) {
        if (configuration.locking()) {
            CompositeKey poolKey = poolRepo.resourceKey(input.accountKey(), input.provision().poolId());
            LockObject lock = lockRepo.extend(poolKey, CreateLockObject.builder()
                    .holder(provisionRepo.resourceKey(poolKey, input.provision().id()).toString())
                    .duration(configuration.lockingDuration())
                    .build());
            LOGGER.info("Holding lock for {} using {}, expires {}",
                    input.provision().poolId(),
                    input.provision().id(),
                    lock.expiresIn());
        }
        return input;
    }
}
