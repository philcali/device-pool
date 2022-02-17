/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.exception;

/**
 * <p>InvalidInputException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class InvalidInputException extends ClientException {
    private static final long serialVersionUID = -589194868205978540L;

    /**
     * <p>Constructor for InvalidInputException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public InvalidInputException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for InvalidInputException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public InvalidInputException(String message) {
        super(message);
    }
}
