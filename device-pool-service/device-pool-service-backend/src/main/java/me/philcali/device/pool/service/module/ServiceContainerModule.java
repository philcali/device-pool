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
                .packages("me.philcali.device.pool.service.resource")
                .property("jersey.config.server.wadl.disableWadl", true)
                .register(JacksonFeature.class)
                .register(binder);
    }
}
