package me.philcali.device.pool.exceptions;

public class LockingConflictException extends LockingException {
    private static final long serialVersionUID = -4747455747612536959L;

    public LockingConflictException(Throwable ex) {
        super(ex);
    }
}
