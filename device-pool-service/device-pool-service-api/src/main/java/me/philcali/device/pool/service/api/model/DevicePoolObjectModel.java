package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
abstract class DevicePoolObjectModel implements Modifiable {
    abstract CompositeKey key();

    abstract String name();

    @Nullable
    abstract String description();
}
