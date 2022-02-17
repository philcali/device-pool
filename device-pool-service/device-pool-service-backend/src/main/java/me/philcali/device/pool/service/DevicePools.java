/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service;

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
    private final DevicePoolsComponent component;

    /**
     * <p>Constructor for DevicePools.</p>
     *
     * @param component a {@link me.philcali.device.pool.service.module.DevicePoolsComponent} object
     */
    public DevicePools(DevicePoolsComponent component) {
        this.component = component;
    }

    /**
     * <p>Constructor for DevicePools.</p>
     */
    public DevicePools() {
        this(DaggerDevicePoolsComponent.create());
    }

    /** {@inheritDoc} */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        component.handler().proxyStream(inputStream, outputStream, context);
    }
}
