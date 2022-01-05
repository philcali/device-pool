package me.philcali.device.pool.service.exception;

public class WorkflowExecutionException extends Exception {
    private static final long serialVersionUID = -3470881710578041729L;

    public WorkflowExecutionException(Throwable ex) {
        super(ex);
    }

    public WorkflowExecutionException(String message) {
        super(message);
    }
}
