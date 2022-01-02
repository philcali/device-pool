package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FailProvisionStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private final ProvisionRepo provisionRepo;

    @Inject
    public FailProvisionStep(final ProvisionRepo provisionRepo) {
        this.provisionRepo = provisionRepo;
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        return null;
    }
}
