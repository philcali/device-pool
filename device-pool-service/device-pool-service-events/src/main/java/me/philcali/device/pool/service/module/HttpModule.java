/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import java.net.http.HttpClient;

@Module
class HttpModule {
    @Provides
    @Singleton
    static HttpClient providesHttpClient() {
        return HttpClient.newHttpClient();
    }
}
