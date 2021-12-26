package me.philcali.device.pool.service.exception;

public class RetryableException extends RuntimeException {
    private static final long serialVersionUID = 3374954329983921116L;

    public RetryableException(Throwable ex) {
        super(ex);
    }
}
