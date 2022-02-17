/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;

/**
 * <p>WorkflowStep interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface WorkflowStep<I, O> extends ListAllMixin {
    /**
     * <p>execute.</p>
     *
     * @param input a I object
     * @return a O object
     * @throws me.philcali.device.pool.service.exception.WorkflowExecutionException if any.
     * @throws me.philcali.device.pool.service.exception.RetryableException if any.
     */
    O execute(I input) throws WorkflowExecutionException, RetryableException;
}
