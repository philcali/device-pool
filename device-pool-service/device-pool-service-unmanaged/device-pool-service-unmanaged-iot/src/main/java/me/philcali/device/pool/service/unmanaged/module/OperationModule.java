/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.module;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.unmanaged.Configuration;
import me.philcali.device.pool.service.unmanaged.model.LockingConfiguration;
import me.philcali.device.pool.service.unmanaged.operation.LockingOperationFunction;
import me.philcali.device.pool.service.unmanaged.operation.ObtainDeviceFunction;

import javax.inject.Singleton;
import java.util.function.Function;

@Module
class OperationModule {
    @Provides
    @Singleton
    Configuration providesConfiguration() {
        return Configuration.create();
    }

    @Provides
    @Singleton
    LockingConfiguration providesLockingConfiguration(Configuration configuration) {
        return configuration;
    }

    @Provides
    @IntoMap
    @StringKey("ObtainDevice")
    Function providesObtainDeviceFunction(
            LockingOperationFunction<ObtainDeviceRequest> locking,
            ObtainDeviceFunction function) {
        return locking.andThen(function);
    }
}
