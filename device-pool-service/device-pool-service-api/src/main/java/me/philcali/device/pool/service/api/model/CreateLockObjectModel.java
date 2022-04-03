/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateLockObject.class)
abstract class CreateLockObjectModel {
    abstract String holder();

    @Nullable
    abstract String value();

    @Nullable
    abstract Duration duration();

    @Nullable
    abstract Instant expiresIn();

    @Value.Check
    CreateLockObjectModel validate() {
        if (Objects.nonNull(expiresIn())) {
            return this;
        }
        if (Objects.isNull(duration()) && Objects.isNull(expiresIn())) {
            throw new IllegalStateException("CreateLockObject needs either a duration or expiresIn");
        }
        return CreateLockObject.builder()
                .from(this)
                .expiresIn(Instant.now().plus(duration()).truncatedTo(ChronoUnit.SECONDS))
                .build();
    }
}
