package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.List;

@ApiModel
@Value.Immutable
interface ProvisionOutputModel {
    String id();

    List<Reservation> reservations();

    @Value.Default
    default boolean succeeded() {
        return status() == Status.SUCCEEDED;
    }

    @Value.Default
    default Status status() {
        Status status = Status.REQUESTED;
        for (Reservation reservation : reservations()) {
            if (reservation.status().isTerminal()) {
                if (reservation.status() == Status.FAILED) {
                    return Status.FAILED;
                }
                status = reservation.status();
            } else {
                return Status.PROVISIONING;
            }
        }
        return status;
    }
}
