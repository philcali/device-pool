/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;


import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import java.time.Duration;

@ApiModel
@Value.Immutable
abstract class  ConfigurationModel {
    private static final String ENV_THING_GROUP_NAME = "THING_GROUP_NAME";
    private static final String ENV_ENABLE_RECURSION = "ENABLE_RECURSION";
    private static final String ENV_ENABLE_LOCKING = "ENABLE_LOCKING";
    private static final String ENV_LOCKING_DURATION = "LOCKING_DURATION";

    public static Configuration create() {
        return Configuration.builder().build();
    }

    @Value.Default
    String thingGroup() {
        return System.getenv(ENV_THING_GROUP_NAME);
    }

    @Value.Default
    boolean recursive() {
        return Boolean.parseBoolean(System.getenv(ENV_ENABLE_RECURSION));
    }

    @Value.Default
    boolean locking() {
        return Boolean.parseBoolean(System.getenv(ENV_ENABLE_LOCKING));
    }

    @Value.Default
    Duration lockingDuration() {
        return Duration.ofSeconds(Long.parseLong(System.getenv(ENV_LOCKING_DURATION)));
    }
}
