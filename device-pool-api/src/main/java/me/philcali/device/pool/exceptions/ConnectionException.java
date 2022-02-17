/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

import me.philcali.device.pool.model.CommandInput;

/**
 * <p>ConnectionException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class ConnectionException extends RuntimeException {
    private static final long serialVersionUID = -8046163299450391791L;
    private int errorCode = -1;
    private CommandInput originalInput;

    /**
     * <p>Constructor for ConnectionException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public ConnectionException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for ConnectionException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public ConnectionException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for ConnectionException.</p>
     *
     * @param exitCode a int
     * @param message a {@link java.lang.String} object
     * @param originalInput a {@link me.philcali.device.pool.model.CommandInput} object
     */
    public ConnectionException(int exitCode, String message, CommandInput originalInput) {
        super(message);
        this.errorCode = exitCode;
        this.originalInput = originalInput;
    }

    /**
     * <p>errorCode.</p>
     *
     * @return a int
     */
    public int errorCode() {
        return errorCode;
    }

    /**
     * <p>originalInput.</p>
     *
     * @return a {@link me.philcali.device.pool.model.CommandInput} object
     */
    public CommandInput originalInput() {
        return originalInput;
    }
}
