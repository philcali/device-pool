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
