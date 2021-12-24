package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateReservationObject.class)
interface CreateReservationObjectModel {
    String id();

    String deviceId();
}
