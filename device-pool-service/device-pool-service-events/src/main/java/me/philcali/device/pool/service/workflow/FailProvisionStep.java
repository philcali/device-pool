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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FailProvisionStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private static final Logger LOGGER = LogManager.getLogger(FailProvisionStep.class);
    private final ProvisionRepo provisionRepo;

    @Inject
    public FailProvisionStep(final ProvisionRepo provisionRepo) {
        this.provisionRepo = provisionRepo;
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        try {
            ProvisionObject provisionObject = provisionRepo.update(input.key(), UpdateProvisionObject.builder()
                    .status(Status.FAILED)
                    .message(input.error())
                    .id(input.provision().id())
                    .build());
            return WorkflowState.builder()
                    .from(input)
                    .provision(provisionObject)
                    .build();
        } catch (NotFoundException e) {
            LOGGER.warn("Failed to find provision with id {}", input.provision().selfKey());
        } catch (ServiceException e) {
            LOGGER.error("Failed to update provision {} due to failure, retrying",
                    input.provision().selfKey(), e);
            throw new RetryableException(e);
        }
        return input;
    }
}
