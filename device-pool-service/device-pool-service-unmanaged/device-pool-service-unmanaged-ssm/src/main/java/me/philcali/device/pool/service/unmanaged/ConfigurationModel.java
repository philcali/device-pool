/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.unmanaged.model.LockingConfiguration;
import org.immutables.value.Value;

import java.time.Duration;

@ApiModel
@Value.Immutable
public abstract class ConfigurationModel implements LockingConfiguration {
    private static final String ENV_PROVISION_STRATEGY = "PROVISION_STRATEGY";
    private static final String ENV_LOCKING = "LOCKING";
    private static final String ENV_LOCKING_DURATION = "LOCKING_DURATION";

    public static Configuration create() {
        return Configuration.builder().build();
    }

    @Value.Default
    ProvisionStrategy provisionStrategy() {
        return ProvisionStrategy.valueOf(System.getenv(ENV_PROVISION_STRATEGY));
    }

    @Value.Default
    public boolean locking() {
        return Boolean.parseBoolean(System.getenv(ENV_LOCKING));
    }

    @Value.Default
    public Duration lockingDuration() {
        return Duration.ofSeconds(Long.parseLong(System.getenv(ENV_LOCKING_DURATION)));
    }
}
