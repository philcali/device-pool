/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
interface HostModel {
    PlatformOS platform();

    String hostName();

    @Value.Default
    default Integer port() {
        return 22;
    }

    @Nullable
    String proxyJump();

    String deviceId();
}
