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
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateProvisionObject.class)
interface CreateProvisionObjectModel {
    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String id();

    /**
     * <p>amount.</p>
     *
     * @return a int
     */
    @Value.Default
    default int amount() {
        return 1;
    }

    /**
     * <p>expiresIn.</p>
     *
     * @return a {@link java.time.Instant} object
     */
    @Nullable
    Instant expiresIn();
}
