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
@JsonDeserialize(as = CreateDeviceObject.class)
interface CreateDeviceObjectModel {
    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String id();

    /**
     * <p>privateAddress.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String privateAddress();

    /**
     * <p>publicAddress.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String publicAddress();

    /**
     * <p>expiresIn.</p>
     *
     * @return a {@link java.time.Instant} object
     */
    @Nullable
    Instant expiresIn();
}
