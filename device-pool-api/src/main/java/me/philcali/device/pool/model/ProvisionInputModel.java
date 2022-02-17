/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.UUID;

@ApiModel
@Value.Immutable
interface ProvisionInputModel {
    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Value.Default
    default String id() {
        return UUID.randomUUID().toString();
    }

    /**
     * <p>amount.</p>
     *
     * @return a int
     */
    @Value.Default
    default int amount() {
        return 1;
    }
}
