package me.philcali.device.pool.exceptions;

public class ReservationException extends RuntimeException {
    private static final long serialVersionUID = -4905520719802208611L;

    public ReservationException(Throwable ex) {
        super(ex);
    }
}
