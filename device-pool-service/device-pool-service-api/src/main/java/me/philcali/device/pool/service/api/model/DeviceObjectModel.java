package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;

@ApiModel
@Value.Immutable
@JsonSerialize(as = DeviceObject.class)
interface DeviceObjectModel extends Modifiable {
    @JsonIgnore
    CompositeKey key();

    @Value.Default
    default String poolId() {
        // "ResourceName:id:Subresource"
        return key().resources().get(key().resources().size() - 2);
    }

    default boolean expires() {
        return Objects.nonNull(expiresIn());
    }

    String id();

    @Nullable
    String publicAddress();

    @Nullable
    String privateAddress();

    @Nullable
    Instant expiresIn();
}
