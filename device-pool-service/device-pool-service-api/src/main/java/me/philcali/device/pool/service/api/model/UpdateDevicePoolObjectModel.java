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

@ApiModel
@Value.Immutable
@JsonDeserialize(as = UpdateDevicePoolObject.class)
interface UpdateDevicePoolObjectModel {
    /**
     * <p>name.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String name();

    /**
     * <p>description.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String description();

    /**
     * <p>type.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.DevicePoolType} object
     */
    @Nullable
    DevicePoolType type();

    /**
     * <p>endpoint.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.DevicePoolEndpoint} object
     */
    @Nullable
    DevicePoolEndpoint endpoint();

    /**
     * <p>lockOptions.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.DevicePoolLockOptions} object
     */
    @Nullable
    DevicePoolLockOptions lockOptions();
}
