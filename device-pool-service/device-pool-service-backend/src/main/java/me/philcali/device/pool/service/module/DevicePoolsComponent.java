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

/**
 * <p>DevicePoolsComponent interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Component(modules = {
        JacksonModule.class,
        ServiceContainerModule.class,
        DynamoDBModule.class
})
@Singleton
public interface DevicePoolsComponent {
    /**
     * <p>handler.</p>
     *
     * @return a {@link com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler} object
     */
    JerseyLambdaContainerHandler handler();

    /**
     * <p>application.</p>
     *
     * @return a {@link org.glassfish.jersey.server.ResourceConfig} object
     */
    ResourceConfig application();
}
