package me.philcali.device.pool.service.rpc.exception;

public class RemoteServiceException extends RuntimeException {
    public RemoteServiceException(Throwable ex) {
        super(ex);
    }
}
