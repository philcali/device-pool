/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import me.philcali.device.pool.service.workflow.Constants;
import me.philcali.device.pool.service.workflow.FinishProvisionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
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
import java.util.Map;

@Singleton
public class StartProvisionWorkflowFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartProvisionWorkflowFunction.class);
    private final String workflowId;
    private final SfnClient states;
    private final ObjectMapper mapper;
    private final DevicePoolRepo poolRepo;
    private final TableSchema<ProvisionObject> provisionSchema;
    private final FinishProvisionStep finishStep;

    @Inject
    StartProvisionWorkflowFunction(
            @Named(Constants.WORKFLOW_ID) String workflowId,
            SfnClient states,
            DevicePoolRepo poolRepo,
            ObjectMapper mapper,
            TableSchema<ProvisionObject> provisionSchema,
            FinishProvisionStep finishStep) {
        this.workflowId = workflowId;
        this.states = states;
        this.poolRepo = poolRepo;
        this.mapper = mapper;
        this.provisionSchema = provisionSchema;
        this.finishStep = finishStep;
    }

    @Override
    public boolean test(Record record) {
        return record.getEventName().equals(OperationType.INSERT.name())
                && primaryKey(record).endsWith(ProvisionRepoDynamo.RESOURCE);
    }

    @Override
    public void accept(
            Map<String, AttributeValue> newImage,
            Map<String, AttributeValue> oldImage) {
        ProvisionObject provisionObject = provisionSchema.mapToItem(newImage);
        WorkflowState input = WorkflowState.of(provisionObject.key(), provisionObject);
        try {
            String executionId = execute(input);
            LOGGER.info("Initiated execution with id {}", executionId);
        } catch (WorkflowExecutionException e) {
            LOGGER.error("Failed to initiate workflow {} for {}", workflowId, input, e);
            try {
                finishStep.execute(input.fail("Failed to start workflow: " + e.getMessage()));
            } catch (WorkflowExecutionException inner) {
                LOGGER.error("Failed to mark provision as failed {}", provisionObject, e);
            }
        }
    }

    private String execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        try {
            DevicePoolObject pool = poolRepo.get(CompositeKey.of(input.key().account()), input.provision().poolId());
            final StartExecutionResponse response = states.startExecution(StartExecutionRequest.builder()
                    .stateMachineArn(workflowId)
                    .input(mapper.writeValueAsString(WorkflowState.builder()
                            .from(input)
                            .poolType(pool.type())
                            .endpoint(pool.endpoint())
                            .poolLockOptions(pool.lockOptions())
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
