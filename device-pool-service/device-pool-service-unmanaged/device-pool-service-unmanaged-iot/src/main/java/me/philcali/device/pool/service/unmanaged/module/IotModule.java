/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.module;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.iot.IotClient;

import javax.inject.Singleton;

@Module
class IotModule {
    @Provides
    @Singleton
    static IotClient providesIotClient() {
        return IotClient.create();
    }
}
