/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

@ApiModel
@Value.Immutable
abstract class PlatformOSModel {
    /**
     * <p>os.</p>
     *
     * @return a {@link java.lang.String} object
     */
    abstract String os();

    /**
     * <p>arch.</p>
     *
     * @return a {@link java.lang.String} object
     */
    abstract String arch();

    public static PlatformOS fromString(String platform) {
        String[] parts = platform.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("PlatformOS format is 'os:arch' but " + platform + " was provided");
        }
        return PlatformOS.of(parts[0], parts[1]);
    }

    @Override
    public String toString() {
        return os() + ":" + arch();
    }

    /**
     * <p>isWindows.</p>
     *
     * @return a boolean
     */
    public boolean isWindows() {
        return os().equalsIgnoreCase("windows");
    }
}
