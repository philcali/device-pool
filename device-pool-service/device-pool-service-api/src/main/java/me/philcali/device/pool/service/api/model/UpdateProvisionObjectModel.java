/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = UpdateProvisionObject.class)
interface UpdateProvisionObjectModel {
    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String id();

    /**
     * <p>executionId.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String executionId();

    /**
     * <p>status.</p>
     *
     * @return a {@link me.philcali.device.pool.model.Status} object
     */
    Status status();

    /**
     * <p>message.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String message();

    /**
     * <p>expiresIn.</p>
     *
     * @return a {@link java.time.Instant} object
     */
    @Nullable
    Instant expiresIn();
}
