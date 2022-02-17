/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.client.exception;

/**
 * <p>DeviceLabServiceException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class DeviceLabServiceException extends RuntimeException {
    private static final long serialVersionUID = -2094360147746649774L;

    private int code;

    /**
     * <p>Constructor for DeviceLabServiceException.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param code a int
     */
    public DeviceLabServiceException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * <p>code.</p>
     *
     * @return a int
     */
    public int code() {
        return code;
    }
}
