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
    @Nullable
    String name();

    @Nullable
    String description();

    @Nullable
    DevicePoolType type();

    @Nullable
    DevicePoolEndpoint endpoint();

    @Nullable
    DevicePoolLockOptions lockOptions();
}
