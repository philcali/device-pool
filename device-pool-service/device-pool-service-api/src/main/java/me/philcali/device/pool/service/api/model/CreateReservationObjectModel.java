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

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateReservationObject.class)
interface CreateReservationObjectModel {
    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String id();

    /**
     * <p>deviceId.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String deviceId();

    /**
     * <p>status.</p>
     *
     * @return a {@link me.philcali.device.pool.model.Status} object
     */
    @Value.Default
    default Status status() {
        return Status.REQUESTED;
    }
}
