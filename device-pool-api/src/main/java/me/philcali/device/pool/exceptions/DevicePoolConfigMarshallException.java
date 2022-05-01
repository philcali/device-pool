/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

public class DevicePoolConfigMarshallException extends RuntimeException {
    private static final long serialVersionUID = 8321219845468923868L;

    public DevicePoolConfigMarshallException(Throwable ex) {
        super(ex);
    }
}
