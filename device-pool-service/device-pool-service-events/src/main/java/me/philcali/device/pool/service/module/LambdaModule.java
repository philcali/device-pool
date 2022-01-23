/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.lambda.LambdaClient;

import javax.inject.Singleton;

@Module
class LambdaModule {
    @Provides
    @Singleton
    static LambdaClient providesLambdaClient() {
        return LambdaClient.create();
    }
}
