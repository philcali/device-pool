/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import me.philcali.device.pool.service.unmanaged.module.DaggerUnmanagedComponent;
import me.philcali.device.pool.service.unmanaged.module.UnmanagedComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ObtainDevicesSSM implements RequestStreamHandler {
    private static final String OPERATION_NAME = "operationName";
    private final UnmanagedComponent component;

    public ObtainDevicesSSM(UnmanagedComponent component) {
        this.component = component;
    }

    public ObtainDevicesSSM() {
        this(DaggerUnmanagedComponent.create());
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        String operationName = context.getClientContext().getCustom().get(OPERATION_NAME);
        component.handler().accept(operationName, inputStream, outputStream);
    }
}
