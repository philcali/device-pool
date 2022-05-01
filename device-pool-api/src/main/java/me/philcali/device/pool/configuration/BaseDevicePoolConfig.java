/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.model.APIShadowModel;
import org.immutables.value.Value;

import java.util.Map;

@APIShadowModel
@Value.Immutable
public abstract class BaseDevicePoolConfig implements DevicePoolConfig {
    @Value.Default
    public String poolClassName() {
        return BaseDevicePool.class.getName();
    }

    public abstract Map<String, DevicePoolConfigEntry> properties();

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ImmutableBaseDevicePoolConfig.Builder {
        public Builder addEntry(DevicePoolConfig.DevicePoolConfigEntry entry) {
            return putProperties(entry.key(), entry);
        }
    }
}
