package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
abstract class DeviceLockObjectModel implements Modifiable, UniqueEntity {
    @Nullable
    abstract String provisionId();

    @Nullable
    abstract String reservationId();

    @Nullable
    abstract Instant expiresIn();
}
