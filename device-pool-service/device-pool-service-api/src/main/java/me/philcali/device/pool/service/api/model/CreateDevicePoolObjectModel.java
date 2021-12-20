package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateDevicePoolObject.class)
interface CreateDevicePoolObjectModel {
    String name();

    @Nullable
    String description();
}
