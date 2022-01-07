package me.philcali.device.pool.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = ErrorRaw.class)
interface ErrorRawModel {
    @JsonProperty("Error")
    String error();

    @JsonProperty("Cause")
    String cause();
}
