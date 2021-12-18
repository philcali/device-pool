package me.philcali.device.pool.service.module;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import dagger.Component;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;

@Component(modules = {
        JacksonModule.class,
        ServiceContainerModule.class,
        DynamoDBModule.class
})
@Singleton
public interface DevicePoolsComponent {
    JerseyLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler();

    ResourceConfig application();
}
