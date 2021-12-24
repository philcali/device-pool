package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = UpdateReservationObject.class)
interface UpdateReservationObjectModel {
    @Nullable
    String id();

    @Nullable
    Status status();

    @Nullable
    String message();
}
