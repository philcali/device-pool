package me.philcali.device.pool.service.api.exception;

public class ClientException extends RuntimeException {
    private static final long serialVersionUID = 2028656939478490270L;

    public ClientException(Throwable ex) {
        super(ex);
    }

    public ClientException(String message) {
        super(message);
    }
}
