/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service;

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import me.philcali.device.pool.service.module.DaggerDevicePoolsComponent;
import me.philcali.device.pool.service.module.DevicePoolsComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>DevicePools class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class DevicePools implements RequestStreamHandler {
    private static final DevicePoolsComponent component;
    private static final JerseyLambdaContainerHandler handler;

    static {
        component = DaggerDevicePoolsComponent.create();
        handler = component.handler();
        component.coldStartTrigger().run();
    }

    /** {@inheritDoc} */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
