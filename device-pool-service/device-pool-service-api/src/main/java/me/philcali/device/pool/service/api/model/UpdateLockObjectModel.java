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
@JsonDeserialize(as = UpdateLockObject.class)
interface UpdateLockObjectModel {
    /**
     * Optional. An arbitrary value associated with the lock.
     *
     * @return a {@link java.lang.String} value
     */
    @Nullable
    String value();

    /**
     * <p>holder.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String holder();

    /**
     * <p>expiresIn.</p>
     *
     * @return a {@link java.time.Instant} object
     */
    @Nullable
    Instant expiresIn();
}
