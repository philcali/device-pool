/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StopExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StopExecutionResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;

@Singleton
public class CancelProvisionWorkflowFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LogManager.getLogger(CancelProvisionWorkflowFunction.class);
    private final ProvisionRepo provisionRepo;
    private final ReservationRepo reservationRepo;
    private final SfnClient sfn;
    private final TableSchema<ProvisionObject> provisionSchema;

    @Inject
    public CancelProvisionWorkflowFunction(
            final ProvisionRepo provisionRepo,
            final ReservationRepo reservationRepo,
            final SfnClient sfn,
            final TableSchema<ProvisionObject> provisionSchema) {
        this.provisionRepo = provisionRepo;
        this.reservationRepo = reservationRepo;
        this.sfn = sfn;
        this.provisionSchema = provisionSchema;
    }

    @Override
    public boolean test(Record record) {
        // A provision was modified to canceling from a non-canceled status.
        return record.getEventName().equals(OperationType.MODIFY.name())
                && primaryKey(record).endsWith(ProvisionRepoDynamo.RESOURCE)
                && record.getDynamodb().getNewImage().get("status").getS().equals(Status.CANCELING.name())
                && !record.getDynamodb().getOldImage().get("status").getS().equals(Status.CANCELING.name());
    }

    @Override
    public void accept(
            Map<String, AttributeValue> newImage,
            Map<String, AttributeValue> oldImage) {
        ProvisionObject provision = provisionSchema.mapToItem(newImage);
        try {
            ProvisionObject updated = execute(provision);
            LOGGER.info("Canceled provision {}", updated);
        } catch (NotFoundException | InvalidInputException e) {
            LOGGER.warn("Provision no longer exists {}", provision.id());
        } catch (WorkflowExecutionException e) {
            LOGGER.error("Failed to cancel execution for {}", provision, e);
        }
    }

    private ProvisionObject execute(ProvisionObject input) throws WorkflowExecutionException, RetryableException {
        if (Objects.nonNull(input.executionId())) {
            try {
                StopExecutionResponse response = sfn.stopExecution(StopExecutionRequest.builder()
                        .executionArn(input.executionId())
                        .build());
                LOGGER.info("Stopped a provision execution on {}", response.stopDate());
            } catch (ResourceNotFoundException e) {
                throw new WorkflowExecutionException(e);
            } catch (SfnException e) {
                if (e.isThrottlingException() || e.statusCode() >= 500) {
                    throw new RetryableException(e);
                }
                throw new WorkflowExecutionException(e);
            }
        }
        listAll(input.selfKey(), reservationRepo).forEach(reservationObject -> {
            // Terminal statuses can't be reset
            if (!reservationObject.status().isTerminal()) {
                reservationRepo.update(input.selfKey(), UpdateReservationObject.builder()
                        .id(reservationObject.id())
                        .status(Status.CANCELING)
                        .build());
            }
        });
        return provisionRepo.update(input.key().parentKey(), UpdateProvisionObject.builder()
                .id(input.id())
                .status(Status.CANCELED)
                .build());
    }
}
