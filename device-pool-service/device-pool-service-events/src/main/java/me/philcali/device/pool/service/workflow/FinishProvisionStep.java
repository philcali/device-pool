/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class FinishProvisionStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private static final Logger LOGGER = LogManager.getLogger(FinishProvisionStep.class);
    private final ProvisionRepo provisionRepo;
    private final ReservationRepo reservationRepo;

    @Inject
    public FinishProvisionStep(
            final ProvisionRepo provisionRepo,
            final ReservationRepo reservationRepo) {
        this.provisionRepo = provisionRepo;
        this.reservationRepo = reservationRepo;
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        UpdateProvisionObject.Builder update = UpdateProvisionObject.builder()
                .id(input.provision().id())
                .status(input.provision().status())
                .message(input.provision().message());
        try {
            List<ReservationObject> reservationObjects = listAll(input.provision().selfKey(), reservationRepo);
            if (reservationObjects.isEmpty() && !input.provision().status().equals(Status.FAILED)) {
                update.status(Status.FAILED).message("Provision completed without reservations");
            } else {
                for (ReservationObject reservationObject : reservationObjects) {
                    if (reservationObject.status() == Status.FAILED) {
                        update.status(Status.FAILED).message("Reservations did not complete successfully");
                        break;
                    }
                }
            }
            return WorkflowState.builder()
                    .from(input)
                    .provision(provisionRepo.update(input.key().parentKey(), update.build()))
                    .build();
        } catch (NotFoundException e) {
            LOGGER.error("Provision with key {} was not found", input.provision().selfKey());
            throw new WorkflowExecutionException(e);
        } catch (ServiceException e) {
            LOGGER.error("Failed to update provision {}, retrying", input.provision().selfKey(), e);
            throw new RetryableException(e);
        }
    }
}
