package me.philcali.device.pool.exceptions;

public class DeviceInteractionException extends RuntimeException {
    private static final long serialVersionUID = 4263367828437780607L;

    public DeviceInteractionException(Throwable ex) {
        super(ex);
    }
}
