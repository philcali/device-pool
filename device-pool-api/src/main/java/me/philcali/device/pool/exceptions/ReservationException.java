/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

public class ReservationException extends RuntimeException {
    private static final long serialVersionUID = -4905520719802208611L;

    public ReservationException(Throwable ex) {
        super(ex);
    }

    public ReservationException(String message) {
        super(message);
    }
}
