package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonSerialize(as = ProvisionObject.class)
abstract class ProvisionObjectModel implements Modifiable, UniqueEntity {
    @JsonIgnore
    @Value.Default
    CompositeKey poolKey() {
        return CompositeKey.builder()
                .account(key().account())
                .resources(key().resources().subList(0, 1))
                .build();
    }

    @Value.Default
    String poolId() {
        // "ResourceName:id:Subresource"
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
