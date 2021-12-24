package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
@JsonSerialize(as = ReservationObject.class)
abstract class ReservationObjectModel implements Modifiable, UniqueEntity {
    abstract String id();

    @Nullable
    abstract String deviceId();

    @Nullable
    abstract Status status();

    @Nullable
    abstract String message();
}
