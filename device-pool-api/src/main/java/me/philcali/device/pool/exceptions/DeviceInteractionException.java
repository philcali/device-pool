/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

public class DeviceInteractionException extends RuntimeException {
    private static final long serialVersionUID = 4263367828437780607L;

    public DeviceInteractionException(Throwable ex) {
        super(ex);
    }
}
