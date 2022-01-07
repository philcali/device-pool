package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonSerialize(as = ProvisionObject.class)
abstract class ProvisionObjectModel implements Modifiable, UniqueEntity {
    @Nullable
    @JsonIgnore
    @Value.Default
    CompositeKey poolKey() {
        if (key() == null) {
            return null;
        }
        return CompositeKey.builder()
                .account(key().account())
                .resources(key().resources().subList(0, 2))
                .build();
    }

    @Nullable
    @Value.Default
    String poolId() {
        if (key() == null) {
            return null;
        }
        return key().resources().get(key().resources().size() - 2);
    }

    abstract Status status();

    @Nullable
    abstract Integer amount();

    @Nullable
    abstract Instant expiresIn();

    @Nullable
    abstract String message();
}
