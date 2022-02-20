/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface DevicePoolConfig {

    String poolClassName();

    Map<String, DevicePoolConfigEntry> properties();

    default Optional<String> get(String key) {
        return Optional.ofNullable(properties().get(key)).flatMap(DevicePoolConfigEntry::value);
    }

    default Optional<DevicePoolConfigEntry> namespace(String namespace) {
        String[] parts = namespace.split("\\.");
        DevicePoolConfigEntry rval = properties().get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (Objects.nonNull(rval)) {
                rval = rval.properties().get(parts[i]);
            }
        }
        return Optional.ofNullable(rval);
    }

    interface DevicePoolConfigEntry {
        String key();

        Optional<String> value();

        default Optional<String> get(String key) {
            return Optional.ofNullable(properties().get(key)).flatMap(DevicePoolConfigEntry::value);
        }

        Map<String, DevicePoolConfigEntry> properties();
    }
}
