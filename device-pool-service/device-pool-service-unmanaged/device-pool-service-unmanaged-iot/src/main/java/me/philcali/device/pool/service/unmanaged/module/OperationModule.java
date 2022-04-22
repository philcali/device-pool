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
import me.philcali.device.pool.service.unmanaged.Configuration;
import me.philcali.device.pool.service.unmanaged.operation.ObtainDeviceFunction;
import me.philcali.device.pool.service.unmanaged.operation.OperationFunction;

import javax.inject.Singleton;

@Module
class OperationModule {
    @Provides
    @Singleton
    static Configuration providesConfiguration() {
        return Configuration.create();
    }

    @Provides
    @IntoMap
    @StringKey("ObtainDevice")
    static OperationFunction providesObtainDeviceFunction(ObtainDeviceFunction function) {
        return function;
    }
}
