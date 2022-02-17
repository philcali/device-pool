/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

/**
 * <p>DeviceInteractionException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class DeviceInteractionException extends RuntimeException {
    private static final long serialVersionUID = 4263367828437780607L;

    /**
     * <p>Constructor for DeviceInteractionException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public DeviceInteractionException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for DeviceInteractionException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public DeviceInteractionException(String message) {
        super(message);
    }
}
