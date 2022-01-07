package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@ApiModel
@Value.Immutable
abstract class CreateDeviceLockObjectModel {
    abstract String id();

    abstract String reservationId();

    abstract String provisionId();

    @Nullable
    abstract Instant expiresIn();

    @Nullable
    abstract Duration duration();

    @Value.Check
    CreateDeviceLockObjectModel validate() {
        if (Objects.isNull(expiresIn()) && Objects.isNull(duration())) {
            throw new IllegalStateException("DeviceLock needs either an expire time or duration");
        }
        if (Objects.nonNull(expiresIn())) {
            return this;
        }
        return CreateDeviceLockObject.builder()
                .from(this)
                .expiresIn(Instant.now().plus(duration()).truncatedTo(ChronoUnit.SECONDS))
                .build();
    }
}
