package me.philcali.device.pool.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ExecutionLimitExceededException;
import software.amazon.awssdk.services.sfn.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class StartStep implements WorkflowStep<WorkflowState, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartStep.class);
    private final String workflowId;
    private final SfnClient states;
    private final ObjectMapper mapper;
    private final DevicePoolRepo poolRepo;

    @Inject
    StartStep(
            @Named(Constants.WORKFLOW_ID) String workflowId,
            SfnClient states,
            DevicePoolRepo poolRepo,
            ObjectMapper mapper) {
        this.workflowId = workflowId;
        this.states = states;
        this.poolRepo = poolRepo;
        this.mapper = mapper;
    }

    @Override
    public String execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        try {
            DevicePoolObject pool = poolRepo.get(CompositeKey.of(input.key().account()), input.provision().poolId());
            final StartExecutionResponse response = states.startExecution(StartExecutionRequest.builder()
                    .stateMachineArn(workflowId)
                    .input(mapper.writeValueAsString(WorkflowState.builder()
                            .from(input)
                            .poolType(pool.type())
                            .endpoint(pool.endpoint())
                            .build()))
                    .build());
            LOGGER.info("Created a new execution for {}: {}", input, response.executionArn());
            return response.executionArn();
        } catch (NotFoundException e) {
            LOGGER.warn("Failed to get pool {}", input.provision().poolId());
            throw new WorkflowExecutionException(e);
        } catch (IOException ie) {
            LOGGER.error("Failed to serialize {}", input, ie);
            throw new WorkflowExecutionException(ie);
        } catch (ResourceNotFoundException e) {
            LOGGER.error("State machine is not found, {}", workflowId, e);
            throw new WorkflowExecutionException(e);
        } catch (ExecutionLimitExceededException e) {
            LOGGER.warn("Execution limit was reached, {}, retrying", workflowId);
            throw new RetryableException(e);
        } catch (SfnException e) {
            if (e.isThrottlingException() || e.statusCode() >= 500) {
                LOGGER.warn("Failed to create a new execution, retrying: {}", input, e);
                throw new RetryableException(e);
            }
            LOGGER.error("Failed to create a new execution, {}", input, e);
            throw new WorkflowExecutionException(e);
        }
    }
}
