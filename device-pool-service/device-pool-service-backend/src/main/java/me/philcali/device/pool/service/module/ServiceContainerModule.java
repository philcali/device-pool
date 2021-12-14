package me.philcali.device.pool.service.module;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import dagger.Module;
import dagger.Provides;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;

@Module
class ServiceContainerModule {
    @Provides
    @Singleton
    static JerseyLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler(ResourceConfig application) {
        return JerseyLambdaContainerHandler.getHttpApiV2ProxyHandler(application);
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
