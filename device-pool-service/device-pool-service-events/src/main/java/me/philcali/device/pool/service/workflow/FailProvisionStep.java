/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.IOException;

/**
 * <p>FailProvisionStep class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Singleton
public class FailProvisionStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private static final Logger LOGGER = LogManager.getLogger(FailProvisionStep.class);
    private final ProvisionRepo provisionRepo;
    private final ObjectMapper mapper;

    @Inject
    /**
     * <p>Constructor for FailProvisionStep.</p>
     *
     * @param provisionRepo a {@link me.philcali.device.pool.service.api.ProvisionRepo} object
     * @param mapper a {@link com.fasterxml.jackson.databind.ObjectMapper} object
     */
    public FailProvisionStep(
            final ProvisionRepo provisionRepo,
            final ObjectMapper mapper) {
        this.provisionRepo = provisionRepo;
        this.mapper = mapper;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        try {
            ProvisionObject provisionObject = provisionRepo.update(input.key().parentKey(), UpdateProvisionObject
                    .builder()
                    .status(Status.FAILED)
                    .message(input.normalizedError(mapper).errorMessage())
                    .id(input.provision().id())
                    .build());
            return WorkflowState.builder()
                    .from(input)
                    .provision(provisionObject)
                    .build();
        } catch (IOException ie) {
            LOGGER.error("Failed to parse error {}", input.error(), ie);
            throw new WorkflowExecutionException(ie);
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
