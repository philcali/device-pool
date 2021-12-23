package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
@JsonSerialize(as = DevicePoolObject.class)
abstract class DevicePoolObjectModel implements Modifiable, UniqueEntity {
    abstract String name();

    @Nullable
    abstract String description();
}
