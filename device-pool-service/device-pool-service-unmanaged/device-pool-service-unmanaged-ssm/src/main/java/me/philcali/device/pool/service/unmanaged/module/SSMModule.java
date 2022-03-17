/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.module;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.inject.Singleton;

@Module
class SSMModule {
    @Provides
    @Singleton
    static SsmClient providesSSMClient() {
        return SsmClient.create();
    }
}
