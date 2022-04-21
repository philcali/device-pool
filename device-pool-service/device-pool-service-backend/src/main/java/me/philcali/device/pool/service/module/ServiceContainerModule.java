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
import org.glassfish.jersey.server.ServerProperties;

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
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .property(ServerProperties.JSON_PROCESSING_FEATURE_DISABLE, true)
                .property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, true)
                .property(ServerProperties.MOXY_JSON_FEATURE_DISABLE, true)
                .property(ServerProperties.BV_FEATURE_DISABLE, true)
                .packages(
                        "me.philcali.device.pool.service.resource",
                        "me.philcali.device.pool.service.exception",
                        "me.philcali.device.pool.service.context")
                .register(JacksonFeature.class)
                .register(binder);
    }
}
