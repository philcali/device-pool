package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
abstract class ReservationObjectModel implements Modifiable {
    abstract CompositeKey account();

    abstract String id();

    abstract String deviceId();

    abstract Status status();
}
