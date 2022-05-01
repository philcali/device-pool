/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.model.APIShadowModel;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@APIShadowModel
@Value.Immutable
public abstract class BaseDevicePoolConfigEntry implements DevicePoolConfig.DevicePoolConfigEntry {
    public abstract String key();

    @Value.Default
    public Optional<String> value() {
        return Optional.empty();
    }

    public abstract Map<String, DevicePoolConfig.DevicePoolConfigEntry> properties();

    public static Builder builder() {
        return new Builder();
    }

    public static DevicePoolConfig.DevicePoolConfigEntry of(String key) {
        return builder().key(key).build();
    }

    public static final class Builder extends ImmutableBaseDevicePoolConfigEntry.Builder {
        public Builder addEntry(DevicePoolConfig.DevicePoolConfigEntry entry) {
            return putProperties(entry.key(), entry);
        }

        public Builder value(String value) {
            return value(Optional.ofNullable(value));
        }
    }
}
