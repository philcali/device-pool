/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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
