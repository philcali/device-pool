package me.philcali.device.pool.service.rpc.exception;

public class RemoteServiceException extends RuntimeException {
    private static final long serialVersionUID = -5964414415826715312L;

    public RemoteServiceException(Throwable ex) {
        super(ex);
    }

    public RemoteServiceException(String message) {
        super(message);
    }
}
