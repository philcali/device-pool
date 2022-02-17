/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
interface DevicePoolEndpointModel {
    /**
     * <p>uri.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String uri();

    /**
     * <p>type.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.DevicePoolEndpointType} object
     */
    DevicePoolEndpointType type();
}
