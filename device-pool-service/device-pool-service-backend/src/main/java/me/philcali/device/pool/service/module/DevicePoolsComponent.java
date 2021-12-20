package me.philcali.device.pool.service.module;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
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
    JerseyLambdaContainerHandler handler();

    ResourceConfig application();
}
