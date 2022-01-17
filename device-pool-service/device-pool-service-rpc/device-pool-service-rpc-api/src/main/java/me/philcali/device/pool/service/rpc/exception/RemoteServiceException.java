package me.philcali.device.pool.service.rpc.exception;

public class RemoteServiceException extends RuntimeException {
    private static final long serialVersionUID = -5964414415826715312L;
    private final boolean retryable;

    public RemoteServiceException(Throwable ex, boolean retryable) {
        super(ex);
        this.retryable = retryable;
    }

    public RemoteServiceException(Throwable ex) {
        this(ex, false);
    }

    public RemoteServiceException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public RemoteServiceException(String message) {
        this(message, false);
    }

    public boolean isRetryable() {
        return retryable;
    }
}
