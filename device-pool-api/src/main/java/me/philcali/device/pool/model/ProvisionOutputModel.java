/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.List;

@ApiModel
@Value.Immutable
interface ProvisionOutputModel {
    String id();

    List<Reservation> reservations();

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
