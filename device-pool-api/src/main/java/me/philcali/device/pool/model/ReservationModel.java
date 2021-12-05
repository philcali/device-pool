package me.philcali.device.pool.model;

import org.immutables.value.Value;

@ApiModel
@Value.Immutable
interface ReservationModel {
    String deviceId();

    Status status();
}
