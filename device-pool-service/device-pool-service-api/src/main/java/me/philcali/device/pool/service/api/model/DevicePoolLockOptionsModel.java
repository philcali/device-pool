package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import java.time.Duration;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = DevicePoolLockOptions.class)
abstract class DevicePoolLockOptionsModel {
    abstract boolean enabled();

    @Value.Default
    Long initialDuration() {
        return Duration.ofHours(1).toSeconds();
    }
}
