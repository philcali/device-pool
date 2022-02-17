/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.exception;

/**
 * <p>ClientException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class ClientException extends RuntimeException {
    private static final long serialVersionUID = 2028656939478490270L;

    /**
     * <p>Constructor for ClientException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public ClientException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for ClientException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public ClientException(String message) {
        super(message);
    }
}
