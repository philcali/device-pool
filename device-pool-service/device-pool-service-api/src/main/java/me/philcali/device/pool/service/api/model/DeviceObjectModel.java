/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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
    /**
     * <p>poolId.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    @Value.Default
    default String poolId() {
        // "ResourceName:id:Subresource"
        if (Objects.isNull(key())) {
            return null;
        }
        return key().resources().get(key().resources().size() - 2);
    }

    /**
     * <p>poolKey.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     */
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

    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String id();

    /**
     * <p>publicAddress.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String publicAddress();

    /**
     * <p>privateAddress.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String privateAddress();

    /**
     * <p>expiresIn.</p>
     *
     * @return a {@link java.time.Instant} object
     */
    @Nullable
    Instant expiresIn();
}
