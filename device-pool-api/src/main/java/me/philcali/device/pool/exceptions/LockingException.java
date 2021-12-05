package me.philcali.device.pool.exceptions;

public class LockingException extends RuntimeException {
    private static final long serialVersionUID = 6047014952839580825L;

    public LockingException(Throwable ex) {
        super(ex);
    }
}
