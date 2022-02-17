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
interface LockOutputModel {
    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String id();

    /**
     * <p>value.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String value();

    /**
     * <p>updatedAt.</p>
     *
     * @return a long
     */
    long updatedAt();

    /**
     * <p>expiresIn.</p>
     *
     * @return a long
     */
    long expiresIn();
}
