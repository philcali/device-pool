/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Objects;

@ApiModel
@Value.Immutable
@JsonSerialize(as = ReservationObject.class)
abstract class ReservationObjectModel implements Modifiable, UniqueEntity {
    @Nullable
    @JsonIgnore
    @Value.Default
    public CompositeKey provisionKey() {
        if (Objects.isNull(key())) {
            return null;
        }
        return CompositeKey.builder()
                .account(key().account())
                .resources(key().resources().subList(0, 2))
                .build();
    }

    @Nullable
    @JsonIgnore
    @Value.Default
    public String provisionId() {
        if (Objects.isNull(key())) {
            return null;
        }
        return key().resources().get(3);
    }

    @Nullable
    @JsonIgnore
    @Value.Default
    CompositeKey poolKey() {
        if (Objects.isNull(key())) {
            return null;
        }
        return CompositeKey.builder()
                .account(key().account())
                .resources(key().resources().subList(0, 1))
                .build();
    }

    @Nullable
    @Value.Default
    String poolId() {
        if (Objects.isNull(key())) {
            return null;
        }
        return key().resources().get(key().resources().size() - 4);
    }

    @Nullable
    abstract String deviceId();

    @Nullable
    abstract Status status();

    @Nullable
    abstract String message();
}
