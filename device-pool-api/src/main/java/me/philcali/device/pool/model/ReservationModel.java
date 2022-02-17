/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

@ApiModel
@Value.Immutable
interface ReservationModel {
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
    Status status();
}
