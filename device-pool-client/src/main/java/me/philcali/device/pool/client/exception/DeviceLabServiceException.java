/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.client.exception;

public class DeviceLabServiceException extends RuntimeException {
    private static final long serialVersionUID = -2094360147746649774L;

    private int code;

    public DeviceLabServiceException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int code() {
        return code;
    }
}
