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
interface DeviceObjectModel extends Modifiable, UniqueEntity {
    @Nullable
    @Value.Default
    default String poolId() {
        // "ResourceName:id:Subresource"
        if (Objects.isNull(key())) {
            return null;
        }
        return key().resources().get(key().resources().size() - 2);
    }

    @Nullable
    @Value.Default
    @JsonIgnore
    default CompositeKey poolKey() {
        if (Objects.isNull(key())) {
            return null;
        }
        return CompositeKey.builder()
                .account(key().account())
                .resources(key().resources().subList(0, 1))
                .build();
    }

    String id();

    @Nullable
    String publicAddress();

    @Nullable
    String privateAddress();

    @Nullable
    Instant expiresIn();
}
