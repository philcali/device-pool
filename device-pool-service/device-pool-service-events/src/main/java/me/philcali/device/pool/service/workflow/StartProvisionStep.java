package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StartProvisionStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private final ProvisionRepo provisionRepo;

    @Inject
    public StartProvisionStep(final ProvisionRepo provisionRepo) {
        this.provisionRepo = provisionRepo;
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        try {
            return WorkflowState.builder()
                    .from(input)
                    .provision(provisionRepo.update(input.key().parentKey(), UpdateProvisionObject.builder()
                            .id(input.provision().id())
                            .status(Status.PROVISIONING)
                            .executionId(input.executionArn())
                            .build()))
                    .build();
        } catch (NotFoundException e) {
            throw new WorkflowExecutionException(e);
        } catch (ServiceException e) {
            throw new RetryableException(e);
        }
    }
}
