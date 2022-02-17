/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.exception;

/**
 * <p>RetryableException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class RetryableException extends RuntimeException {
    private static final long serialVersionUID = 3374954329983921116L;

    /**
     * <p>Constructor for RetryableException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public RetryableException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for RetryableException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public RetryableException(String message) {
        super(message);
    }
}
