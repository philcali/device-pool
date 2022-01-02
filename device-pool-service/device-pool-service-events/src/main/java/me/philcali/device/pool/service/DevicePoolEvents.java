package me.philcali.device.pool.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.data.TableSchemas;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import me.philcali.device.pool.service.module.DaggerDevicePoolEventComponent;
import me.philcali.device.pool.service.module.DevicePoolEventComponent;
import me.philcali.device.pool.service.workflow.WorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Collectors;

public class DevicePoolEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePoolEvents.class);
    private static final String PK = "PK";
    private final DevicePoolEventComponent component;

    public DevicePoolEvents(DevicePoolEventComponent component) {
        this.component = component;
    }

    public DevicePoolEvents() {
        this(DaggerDevicePoolEventComponent.create());
    }

    private String primaryKey(Record record) {
        return record.getDynamodb().getNewImage().get(PK).getS();
    }

    private AttributeValue convertAttribute(
            com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value) {
        AttributeValue.Builder builder = AttributeValue.builder();
        builder.s(value.getS());
        builder.n(value.getN());
        builder.bool(value.getBOOL());
        if (value.getB() != null ) {
            builder.b(SdkBytes.fromByteBuffer(value.getB()));
        }
        if (value.getL() != null) {
            builder.l(value.getL().stream()
                    .map(this::convertAttribute)
                    .collect(Collectors.toList()));
        }
        if (value.getM() != null) {
            builder.m(value.getM().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            v -> convertAttribute(v.getValue()))));
        }
        return builder.build();
    }

    private Map<String, AttributeValue> convertAttributes(Record record) {
        return record.getDynamodb().getNewImage().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        value -> convertAttribute(value.getValue())));
    }

    /**
     * Kicks off the provision workflow for both managed and unmanaged device pools.
     *
     * @param event DynamoDB entry representing a creation event
     */
    public void handleProvisionCreation(final DynamodbEvent event) {
        TableSchema<ProvisionObject> provisionSchema = TableSchemas.provisionTableSchema();
        event.getRecords().stream()
                .filter(record -> record.getEventName().equals(OperationType.INSERT.name()))
                .filter(record -> primaryKey(record).endsWith(":" + ProvisionRepoDynamo.RESOURCE))
                .peek(record -> LOGGER.debug("Found provision insert: {}", record))
                .map(this::convertAttributes)
                .map(provisionSchema::mapToItem)
                .map(provision -> WorkflowState.of(provision.key(), provision))
                .forEach(state -> {
                    try {
                        String executionId = component.startProvisioning().execute(state);
                        LOGGER.info("Started execution {}", executionId);
                    } catch (WorkflowExecutionException e) {
                        // TODO: handle this better
                        LOGGER.error("Failed to start workflow for {}", state, e);
                    }
                });
    }

    private <T, O> void handleStep(InputStream in, OutputStream out, Class<T> payloadClass, WorkflowStep<T, O> step) {
        try {
            final T result = component.mapper().readValue(in, payloadClass);
            final O executionResult = step.execute(result);
            component.mapper().writeValue(out, executionResult);
        } catch (IOException | WorkflowExecutionException e) {
            LOGGER.error("Failed to handle JSON payload for {}", payloadClass, e);
        }
    }

    /**
     * Maps to create manual reservations step.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     */
    public void createReservationStep(InputStream input, OutputStream output, Context context) {
        context.getLogger().log("Create Reservation Step is invoked");
        handleStep(input, output, WorkflowState.class, component.createReservationStep());
    }

    /**
     * Flags a provision object that it is now provisioning.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     */
    public void startProvisionStep(InputStream input, OutputStream output, Context context) {
        context.getLogger().log("Start Provision Step is invoked");
        handleStep(input, output, WorkflowState.class, component.startProvisionStep());
    }

    /**
     * Terminates a workflow using any error or success condition.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     */
    public void finishProvisionStep(InputStream input, OutputStream output, Context context) {
        context.getLogger().log("Finsih Provision Step is invoked");
        handleStep(input, output, WorkflowState.class, component.finishProvisionStep());
    }

    /**
     * Forces a workflow failure in the catch-all cases.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     */
    public void failProvisionStep(InputStream input, OutputStream output, Context context) {
        context.getLogger().log("Fail Provision Step is invoked");
        handleStep(input, output, WorkflowState.class, component.failProvisionStep());
    }

    public void obtainDevicesStep(InputStream input, OutputStream output, Context context) {

    }
}
