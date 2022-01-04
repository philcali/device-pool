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
