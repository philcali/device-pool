/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

/**
 * <p>LockingException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class LockingException extends RuntimeException {
    private static final long serialVersionUID = 6047014952839580825L;

    /**
     * <p>Constructor for LockingException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public LockingException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for LockingException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public LockingException(String message) {
        super(message);
    }
}
