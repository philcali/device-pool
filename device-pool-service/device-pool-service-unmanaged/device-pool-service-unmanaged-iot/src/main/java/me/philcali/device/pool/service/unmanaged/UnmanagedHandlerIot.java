/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import me.philcali.device.pool.service.unmanaged.module.DaggerUnmanagedComponent;
import me.philcali.device.pool.service.unmanaged.module.UnmanagedComponent;

public class UnmanagedHandlerIot extends AbstractUnmanagedHandler {
    private static final UnmanagedComponent COMPONENT = DaggerUnmanagedComponent.create();

    @Override
    protected OperationHandler handler() {
        return COMPONENT.handler();
    }
}
