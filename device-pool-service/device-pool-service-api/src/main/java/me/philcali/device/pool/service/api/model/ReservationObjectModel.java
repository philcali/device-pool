package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonSerialize(as = ReservationObject.class)
abstract class ReservationObjectModel implements Modifiable {
    @JsonIgnore
    abstract CompositeKey key();

    abstract String id();

    abstract String deviceId();

    abstract Status status();
}