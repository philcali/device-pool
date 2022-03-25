/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.DevicePoolClientFactory;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>ObtainDevicesStep class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Singleton
public class ObtainDevicesStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private static final Logger LOGGER = LogManager.getLogger(ObtainDevicesStep.class);
    private final ReservationRepo reservationRepo;
    private final LockRepo lockRepo;
    private final DeviceRepo deviceRepo;
    private final DevicePoolClientFactory clientFactory;

    /**
     * <p>Constructor for ObtainDevicesStep.</p>
     *
     * @param deviceRepo a {@link me.philcali.device.pool.service.api.DeviceRepo} object
     * @param reservationRepo a {@link me.philcali.device.pool.service.api.ReservationRepo} object
     * @param lockRepo a {@link me.philcali.device.pool.service.api.LockRepo} object
     * @param clientFactory a {@link me.philcali.device.pool.service.rpc.DevicePoolClientFactory} object
     */
    @Inject
    public ObtainDevicesStep(
            final DeviceRepo deviceRepo,
            final ReservationRepo reservationRepo,
            final LockRepo lockRepo,
            final DevicePoolClientFactory clientFactory) {
        this.deviceRepo = deviceRepo;
        this.reservationRepo = reservationRepo;
        this.lockRepo = lockRepo;
        this.clientFactory = clientFactory;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        if (Objects.isNull(input.endpoint())) {
            throw new WorkflowExecutionException("Workflow input is missing required endpoint.");
        }
        Context context = Context.of(input.endpoint());
        DevicePoolClient client = clientFactory.get(input.endpoint().type());
        List<ReservationObject> existing = listAll(input.provision().selfKey(), reservationRepo);
        int finalized = 0;
        Queue<ReservationObject> pendingReservations = new LinkedList<>();
        for (ReservationObject existingReservation : existing) {
            LOGGER.debug("Found existing reservation {}", existingReservation.id());
            if (existingReservation.status().isTerminal()) {
                finalized++;
            } else {
                pendingReservations.offer(existingReservation);
            }
        }
        int updated = finalized;
        for (int time = 0; time < input.provision().amount() - finalized; time++) {
            ReservationObject reservationObject = pendingReservations.poll();
            try {
                // Update existing device, or obtain a new one
                ObtainDeviceResponse response = client.obtainDevice(context, ObtainDeviceRequest.builder()
                        .provision(input.provision())
                        .accountKey(input.key())
                        .reservation(reservationObject)
                        .build());
                LOGGER.info("Response from {}: {}", input.endpoint().uri(), response);
                DeviceObject newDevice = deviceRepo.put(input.provision().poolKey(), response.device());
                LOGGER.info("Updated device {}", newDevice.id());
                // Obtained a new one, create the entry
                if (Objects.isNull(reservationObject)) {
                    String reservationId = UUID.randomUUID().toString();
                    if (response.status() == Status.SUCCEEDED && input.poolLockOptions().enabled()) {
                        try {
                            lockRepo.create(newDevice.selfKey(), CreateLockObject.builder()
                                    .holder(reservationId)
                                    .duration(Duration.ofSeconds(input.poolLockOptions().duration()))
                                    .build());
                            LOGGER.info("Locking device {} for reservation {}", newDevice.id(), reservationId);
                        } catch (ConflictException e) {
                            LOGGER.warn("Obtained a locked a device {} skipping", newDevice.id());
                            continue;
                        }
                    }
                    reservationObject = reservationRepo.create(input.provision().selfKey(), CreateReservationObject.builder()
                            .deviceId(newDevice.id())
                            .id(reservationId)
                            .build());
                    LOGGER.info("Created a new reservation: {}", reservationObject.id());

                }
                // Update with status code and message
                reservationRepo.update(input.provision().selfKey(), UpdateReservationObject.builder()
                        .id(reservationObject.id())
                        .status(response.status())
                        .message(response.message())
                        .build());
                // Add to total finalized
                if (response.status().isTerminal()) {
                    updated++;
                }
            } catch (RemoteServiceException rse) {
                if (rse.isRetryable()) {
                    throw new RetryableException(rse);
                } else {
                    throw new WorkflowExecutionException(rse);
                }
            }
        }
        AtomicReference<Status> currentStatus = new AtomicReference<>(input.provision().status());
        boolean done = updated == input.provision().amount();
        if (done) {
            currentStatus.set(Status.SUCCEEDED);
        }
        return WorkflowState.builder()
                .from(input.update(b -> b.status(currentStatus.get())))
                .done(done)
                .build();
    }
}
