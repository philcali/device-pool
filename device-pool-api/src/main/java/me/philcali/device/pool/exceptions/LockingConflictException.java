/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

/**
 * <p>LockingConflictException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class LockingConflictException extends LockingException {
    private static final long serialVersionUID = -4747455747612536959L;

    /**
     * <p>Constructor for LockingConflictException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public LockingConflictException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for LockingConflictException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public LockingConflictException(String message) {
        super(message);
    }
}
