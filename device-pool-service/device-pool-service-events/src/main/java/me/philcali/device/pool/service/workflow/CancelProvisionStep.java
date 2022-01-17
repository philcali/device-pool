package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StopExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StopExecutionResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class CancelProvisionStep implements WorkflowStep<ProvisionObject, ProvisionObject> {
    private static final Logger LOGGER = LogManager.getLogger(CancelProvisionStep.class);
    private final ProvisionRepo provisionRepo;
    private final ReservationRepo reservationRepo;
    private final SfnClient sfn;

    @Inject
    public CancelProvisionStep(
            final ProvisionRepo provisionRepo,
            final ReservationRepo reservationRepo,
            final SfnClient sfn) {
        this.provisionRepo = provisionRepo;
        this.reservationRepo = reservationRepo;
        this.sfn = sfn;
    }

    @Override
    public ProvisionObject execute(ProvisionObject input) throws WorkflowExecutionException, RetryableException {
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
                reservationRepo.update(reservationObject.key(), UpdateReservationObject.builder()
                        .id(reservationObject.id())
                        .status(Status.CANCELING)
                        .build());
            }
        });
        return provisionRepo.update(input.key(), UpdateProvisionObject.builder()
                .id(input.id())
                .status(Status.CANCELED)
                .build());
    }
}
