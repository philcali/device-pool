/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import java.util.Map;

@ApiModel
@Value.Immutable
abstract class DevicePoolConfigPropertiesModel implements DevicePoolConfig {
    @Override
    abstract public String poolClassName();

    @Override
    abstract public Map<String, DevicePoolConfigEntry> properties();
}
