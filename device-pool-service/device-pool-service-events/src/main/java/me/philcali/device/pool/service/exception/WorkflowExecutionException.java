/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.exception;

/**
 * <p>WorkflowExecutionException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class WorkflowExecutionException extends Exception {
    private static final long serialVersionUID = -3470881710578041729L;

    /**
     * <p>Constructor for WorkflowExecutionException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public WorkflowExecutionException(Throwable ex) {
        super(ex);
    }

    /**
     * <p>Constructor for WorkflowExecutionException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public WorkflowExecutionException(String message) {
        super(message);
    }
}
