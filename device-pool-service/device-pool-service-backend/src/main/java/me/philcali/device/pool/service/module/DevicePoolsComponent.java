/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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
