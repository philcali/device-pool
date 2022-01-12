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
    public CompositeKey deviceKey() {
        if (Objects.isNull(key())) {
            return null;
        }
        return CompositeKey.builder()
                .account(key().account())
                .addResources(deviceId())
                .build();
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
