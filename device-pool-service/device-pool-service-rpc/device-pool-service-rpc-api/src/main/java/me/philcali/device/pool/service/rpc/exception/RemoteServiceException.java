/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.rpc.exception;

/**
 * <p>RemoteServiceException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class RemoteServiceException extends RuntimeException {
    private static final long serialVersionUID = -5964414415826715312L;
    private final boolean retryable;

    /**
     * <p>Constructor for RemoteServiceException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     * @param retryable a boolean
     */
    public RemoteServiceException(Throwable ex, boolean retryable) {
        super(ex);
        this.retryable = retryable;
    }

    /**
     * <p>Constructor for RemoteServiceException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public RemoteServiceException(Throwable ex) {
        this(ex, false);
    }

    /**
     * <p>Constructor for RemoteServiceException.</p>
     *
     * @param message a {@link java.lang.String} object
     * @param retryable a boolean
     */
    public RemoteServiceException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    /**
     * <p>Constructor for RemoteServiceException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public RemoteServiceException(String message) {
        this(message, false);
    }

    /**
     * <p>isRetryable.</p>
     *
     * @return a boolean
     */
    public boolean isRetryable() {
        return retryable;
    }
}
