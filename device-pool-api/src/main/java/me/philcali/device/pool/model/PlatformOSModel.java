package me.philcali.device.pool.model;

import org.immutables.value.Value;

@ApiModel
@Value.Immutable
interface PlatformOSModel {
    String os();

    String arch();

    default boolean isWindows() {
        return os().equalsIgnoreCase("windows");
    }
}
