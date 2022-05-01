/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.BaseDevicePool;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseDevicePoolConfigTest {

    @Test
    void GIVEN_base_config_WHEN_creating_config_THEN_defaults_are_exercised() {
        DevicePoolConfig config = BaseDevicePoolConfig.builder()
                .addEntry(BaseDevicePoolConfigEntry.of("farts"))
                .addEntry(BaseDevicePoolConfigEntry.builder()
                        .key("parent")
                        .value("something")
                        .addEntry(BaseDevicePoolConfigEntry.of("child"))
                        .build())
                .build();

        DevicePoolConfig expected = BaseDevicePoolConfig.builder()
                .poolClassName(BaseDevicePool.class.getName())
                .putProperties("farts", BaseDevicePoolConfigEntry.builder()
                        .key("farts")
                        .value(Optional.empty())
                        .build())
                .putProperties("parent", BaseDevicePoolConfigEntry.builder()
                        .key("parent")
                        .value(Optional.of("something"))
                        .putProperties("child", BaseDevicePoolConfigEntry.builder()
                                .key("child")
                                .value(Optional.empty())
                                .build())
                        .build())
                .build();
        assertEquals(expected, config);
    }
}
