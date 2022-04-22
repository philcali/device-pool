/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.module;

import dagger.Module;
import dagger.Provides;
import me.philcali.device.pool.iot.HostExpansionIoT;
import me.philcali.device.pool.provision.ExpandingHostProvider;
import me.philcali.device.pool.service.unmanaged.Configuration;
import software.amazon.awssdk.services.iot.IotClient;

import javax.inject.Singleton;

@Module
class IotModule {
    @Provides
    @Singleton
    static ExpandingHostProvider.ExpansionFunction providesHostExpansion(Configuration configuration, IotClient iot) {
        return HostExpansionIoT.builder()
                .iot(iot)
                .recursive(configuration.recursive())
                .thingGroup(configuration.thingGroup())
                .build();
    }

    @Provides
    @Singleton
    static IotClient providesIotCLient() {
        return IotClient.create();
    }
}
