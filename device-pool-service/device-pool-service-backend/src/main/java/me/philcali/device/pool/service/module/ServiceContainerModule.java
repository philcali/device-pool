/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import dagger.Module;
import dagger.Provides;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import java.util.Optional;

@Module
class ServiceContainerModule {
    private static final String API_VERSION = "API_VERSION";

    @Provides
    @Singleton
    static JerseyLambdaContainerHandler handler(ResourceConfig application) {
        if (Optional.ofNullable(System.getenv(API_VERSION)).orElse("V1").equals("V1")) {
            return JerseyLambdaContainerHandler.getAwsProxyHandler(application);
        } else {
            return JerseyLambdaContainerHandler.getHttpApiV2ProxyHandler(application);
        }
    }

    @Provides
    @Singleton
    static ResourceConfig application(ApplicationBinder binder) {
        return new ResourceConfig()
                .packages(
                        "me.philcali.device.pool.service.resource",
                        "me.philcali.device.pool.service.exception",
                        "me.philcali.device.pool.service.context")
                .property("jersey.config.server.wadl.disableWadl", true)
                .register(JacksonFeature.class)
                .register(binder);
    }
}
