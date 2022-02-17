/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

@ApiModel
@Value.Immutable
interface PlatformOSModel {
    /**
     * <p>os.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String os();

    /**
     * <p>arch.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String arch();

    /**
     * <p>isWindows.</p>
     *
     * @return a boolean
     */
    default boolean isWindows() {
        return os().equalsIgnoreCase("windows");
    }
}
