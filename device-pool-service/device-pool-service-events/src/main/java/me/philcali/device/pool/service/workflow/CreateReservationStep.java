package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CreateReservationStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private final ReservationRepo reservationRepo;
    private final DeviceRepo deviceRepo;

    @Inject
    public CreateReservationStep(
            final ReservationRepo reservationRepo,
            final DeviceRepo deviceRepo) {
        this.reservationRepo = reservationRepo;
        this.deviceRepo = deviceRepo;
    }

    private Map<String, ReservationObject> provisionalReservations(ProvisionObject provision) {
        return reservationRepo.list(provision.selfKey(), QueryParams.builder()
                        .limit(provision.amount())
                        .build())
                .results()
                .stream()
                .collect(Collectors.toMap(
                        ReservationObject::deviceId,
                        Function.identity()
                ));
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        List<DeviceObject> devices = deviceRepo.list(input.provision().poolKey(), QueryParams.builder()
                .limit(input.provision().amount())
                .build())
                .results();
        Map<String, ReservationObject> reservations = provisionalReservations(input.provision());
        if (devices.size() < input.provision().amount() - reservations.size()) {
            return input.fail("Requesting " + input.provision().amount()
                    + " but " + input.provision().poolId()
                    + " only has " + devices.size());
        }
        for (DeviceObject device : devices) {
            if (reservations.containsKey(device.id())) {
                continue;
            }
            try {
                reservationRepo.create(input.provision().selfKey(), CreateReservationObject.builder()
                        .id(UUID.randomUUID().toString())
                        .deviceId(device.id())
                        .build());
            } catch (ConflictException ce) {
                throw new WorkflowExecutionException(ce);
            } catch (ServiceException se) {
                throw new RetryableException(se);
            }
        }
        return input.update(b -> b.status(Status.PROVISIONING));
    }
}
