/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import me.philcali.device.pool.service.model.WorkflowStateWrapper;
import me.philcali.device.pool.service.module.DaggerDevicePoolEventComponent;
import me.philcali.device.pool.service.module.DevicePoolEventComponent;
import me.philcali.device.pool.service.workflow.WorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

public class DevicePoolEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePoolEvents.class);
    private final DevicePoolEventComponent component;

    public DevicePoolEvents(DevicePoolEventComponent component) {
        this.component = component;
    }

    public DevicePoolEvents() {
        this(DaggerDevicePoolEventComponent.create());
    }

    /**
     * Kicks off the provision workflow for both managed and unmanaged device pools.
     *
     * @param event DynamoDB entry representing a creation event
     */
    public void handleDatabaseEvents(final DynamodbEvent event) {
        component.eventRouter().accept(event);
    }

    private <O> void handleWorkflowStep(
            InputStream in,
            OutputStream out,
            WorkflowStep<WorkflowState, O> step) throws WorkflowExecutionException {
        handleStep(in, out, WorkflowStateWrapper.class, WorkflowStateWrapper::input, step);
    }

    private <T, R, O> void handleStep(
            InputStream in,
            OutputStream out,
            Class<T> payloadClass,
            Function<T, R> transform,
            WorkflowStep<R, O> step) throws WorkflowExecutionException {
        try {
            final T result = component.mapper().readValue(in, payloadClass);
            final O executionResult = step.execute(transform.apply(result));
            component.mapper().writeValue(out, executionResult);
        } catch (IOException e) {
            LOGGER.error("Failed to handle JSON payload for {}", payloadClass, e);
            throw new WorkflowExecutionException(e);
        }
    }

    /**
     * Maps to create manual reservations step.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     * @throws WorkflowExecutionException when invoking the step fails demonstrably
     */
    public void createReservationStep(InputStream input, OutputStream output, Context context)
            throws WorkflowExecutionException {
        context.getLogger().log("Create Reservation Step is invoked");
        handleWorkflowStep(input, output, component.createReservationStep());
    }

    /**
     * Flags a provision object that it is now provisioning.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     * @throws WorkflowExecutionException when invoking the step fails demonstrably
     */
    public void startProvisionStep(InputStream input, OutputStream output, Context context)
            throws WorkflowExecutionException {
        context.getLogger().log("Start Provision Step is invoked");
        handleWorkflowStep(input, output, component.startProvisionStep());
    }

    /**
     * Terminates a workflow using any error or success condition.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     * @throws WorkflowExecutionException when invoking the step fails demonstrably
     */
    public void finishProvisionStep(InputStream input, OutputStream output, Context context)
            throws WorkflowExecutionException {
        context.getLogger().log("Finish Provision Step is invoked");
        handleWorkflowStep(input, output, component.finishProvisionStep());
    }

    /**
     * Forces a workflow failure in the catch-all cases.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     * @throws WorkflowExecutionException when invoking the step fails demonstrably
     */
    public void failProvisionStep(InputStream input, OutputStream output, Context context)
            throws WorkflowExecutionException {
        context.getLogger().log("Fail Provision Step is invoked");
        handleWorkflowStep(input, output, component.failProvisionStep());
    }

    /**
     * Step to obtain devices from a remote source.
     *
     * @param input Lambda input payload
     * @param output Lambda output payload
     * @param context Lambda function invoke context
     * @throws WorkflowExecutionException when invoking the step fails demonstrably
     */
    public void obtainDevicesStep(InputStream input, OutputStream output, Context context)
            throws WorkflowExecutionException {
        context.getLogger().log("Obtain Devices Step is invoked");
        handleWorkflowStep(input, output, component.obtainDevicesStep());
    }
}
