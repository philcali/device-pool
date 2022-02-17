/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

/**
 * <p>ReservationException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class ReservationException extends RuntimeException {
    private static final long serialVersionUID = -4905520719802208611L;

    /**
     * <p>Constructor for ReservationException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public ReservationException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for ReservationException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public ReservationException(String message) {
        super(message);
    }
}
