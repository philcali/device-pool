/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DeviceLockRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CreateDeviceLockObject;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.Error;
import me.philcali.device.pool.service.model.WorkflowState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class CreateReservationStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private static final Logger LOGGER = LogManager.getLogger(CreateReservationStep.class);
    private final ReservationRepo reservationRepo;
    private final DeviceRepo deviceRepo;
    private final DeviceLockRepo deviceLockRepo;

    @Inject
    public CreateReservationStep(
            final ReservationRepo reservationRepo,
            final DeviceRepo deviceRepo,
            final DeviceLockRepo deviceLockRepo) {
        this.reservationRepo = reservationRepo;
        this.deviceRepo = deviceRepo;
        this.deviceLockRepo = deviceLockRepo;
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
        List<DeviceObject> devices = listAll(input.provision().poolKey(), deviceRepo);
        Map<String, ReservationObject> reservations = provisionalReservations(input.provision());
        if (devices.size() < input.provision().amount() - reservations.size()) {
            return input.fail("Requesting " + input.provision().amount()
                                    + " but " + input.provision().poolId()
                                    + " only has " + devices.size());
        }
        int finalized = reservations.size();
        for (DeviceObject device : devices) {
            // Have enough, done
            if (finalized >= input.provision().amount()) {
                break;
            }
            // Already locked, sipping
            if (reservations.containsKey(device.id())) {
                LOGGER.info("Device {} is already reserved", device.id());
                continue;
            }
            try {
                String reservationId = UUID.randomUUID().toString();
                if (input.poolLockOptions().enabled()) {
                    try {
                        deviceLockRepo.create(device.selfKey(), CreateDeviceLockObject.builder()
                                .reservationId(reservationId)
                                .provisionId(input.provision().id())
                                .id("lock")
                                .duration(Duration.ofSeconds(input.poolLockOptions().initialDuration()))
                                .build());
                        LOGGER.info("Obtained a lock on the device");
                    } catch (ConflictException ce) {
                        LOGGER.info("Device {} is locked, skipping", device.id());
                        continue;
                    }
                }
                reservationRepo.create(input.provision().selfKey(),
                        CreateReservationObject.builder()
                                .id(reservationId)
                                .deviceId(device.id())
                                .status(Status.SUCCEEDED)
                                .build());
                finalized++;
            } catch (ConflictException ce) {
                // This should not happen
                throw new WorkflowExecutionException(ce);
            } catch (ServiceException se) {
                // Make sure we go ahead and release the lock
                deviceLockRepo.delete(device.selfKey(), "lock");
                throw new RetryableException(se);
            }
        }
        final boolean done = finalized >= input.provision().amount();
        return WorkflowState.builder()
                .from(input.update(b -> b.status(done ? Status.SUCCEEDED : Status.PROVISIONING)))
                .done(done)
                .build();
    }
}
