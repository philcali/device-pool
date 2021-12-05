package me.philcali.device.pool.exceptions;

public class ProvisioningException extends RuntimeException {
    private static final long serialVersionUID = 8027212159281679219L;

    public ProvisioningException(String message) {
        super(message);
    }

    public ProvisioningException(Throwable ex) {
        super(ex);
    }
}
