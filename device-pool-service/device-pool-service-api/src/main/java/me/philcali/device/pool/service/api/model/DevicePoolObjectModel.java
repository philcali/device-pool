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

@ApiModel
@Value.Immutable
@JsonSerialize(as = DevicePoolObject.class)
abstract class DevicePoolObjectModel implements Modifiable, UniqueEntity {
    abstract String name();

    /** {@inheritDoc} */
    @JsonIgnore
    @Override
    public String id() {
        return name();
    }

    @Nullable
    abstract String description();

    @Nullable
    abstract DevicePoolType type();

    @Nullable
    abstract DevicePoolEndpoint endpoint();

    @Nullable
    @Value.Default
    DevicePoolLockOptions lockOptions() {
        return DevicePoolLockOptions.of(false);
    }
}
