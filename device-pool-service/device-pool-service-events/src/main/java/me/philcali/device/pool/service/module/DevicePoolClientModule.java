/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.DevicePoolClientFactory;
import me.philcali.device.pool.service.rpc.http.DevicePoolClientHttp;
import me.philcali.device.pool.service.rpc.lambda.DevicePoolClientLambda;

import javax.inject.Singleton;
import java.util.Set;

@Module
class DevicePoolClientModule {
    @Provides @IntoSet
    static DevicePoolClient providesHttpModule(DevicePoolClientHttp http) {
        return http;
    }

    @Provides @IntoSet
    static DevicePoolClient providesLambdaModule(DevicePoolClientLambda lambda) {
        return lambda;
    }

    @Provides
    @Singleton
    static DevicePoolClientFactory providesClientFactory(Set<DevicePoolClient> clients) {
        return DevicePoolClientFactory.fromCollection(clients);
    }
}
