/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.DevicePool;

import java.util.Map;
import java.util.Optional;

public interface DevicePoolConfig {

    String poolClassName();

    Map<String, DevicePoolConfigEntry> properties();

    interface DevicePoolConfigEntry {
        String key();

        Optional<String> value();

        Map<String, DevicePoolConfigEntry> properties();
    }
}
