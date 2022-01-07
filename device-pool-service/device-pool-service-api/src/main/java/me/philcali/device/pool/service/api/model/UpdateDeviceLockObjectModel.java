package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
interface UpdateDeviceLockObjectModel {
    @Nullable
    String id();

    String provisionId();

    String reservationId();

    @Nullable
    Instant expiresIn();
}
