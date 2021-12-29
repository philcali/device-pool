package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class CreateReservationStep implements WorkflowStep<ProvisionObject, ProvisionObject> {
    private final ProvisionRepo provisionRepo;
    private final ReservationRepo reservationRepo;
    private final DeviceRepo deviceRepo;

    @Inject
    public CreateReservationStep(
            final ProvisionRepo provisionRepo,
            final ReservationRepo reservationRepo,
            final DeviceRepo deviceRepo) {
        this.provisionRepo = provisionRepo;
        this.reservationRepo = reservationRepo;
        this.deviceRepo = deviceRepo;
    }

    @Override
    public ProvisionObject execute(ProvisionObject input) throws WorkflowExecutionException, RetryableException {
        List<DeviceObject> devices = deviceRepo.list(input.poolKey(), QueryParams.builder()
                .limit(input.amount())
                .build())
                .results();
        if (devices.size() < input.amount()) {
            return ProvisionObject.builder()
                    .from(input)
                    .status(Status.FAILED)
                    .message("Requesting " + input.amount() + " but " + input.poolId() + " only has " + devices.size())
                    .build();
        }
        devices.forEach(device -> {
            reservationRepo.create(input.selfKey(), CreateReservationObject.builder()
                    .id(UUID.randomUUID().toString())
                    .deviceId(device.id())
                    .build());
        });
        return ProvisionObject.builder()
                .from(input)
                .status(Status.PROVISIONING)
                .build();
    }
}
