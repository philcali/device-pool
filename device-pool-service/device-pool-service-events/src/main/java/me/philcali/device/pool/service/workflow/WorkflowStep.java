package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;

public interface WorkflowStep<I, O> extends ListAllMixin {
    O execute(I input) throws WorkflowExecutionException, RetryableException;
}
