package me.philcali.device.pool.service.api.exception;

public class InvalidInputException extends RuntimeException {
    private static final long serialVersionUID = -589194868205978540L;

    public InvalidInputException(Throwable ex) {
        super(ex);
    }

    public InvalidInputException(String message) {
        super(message);
    }
}
