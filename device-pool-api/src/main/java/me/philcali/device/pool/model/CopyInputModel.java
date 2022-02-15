/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.Collections;
import java.util.Set;

/**
 * Input model for file transfer commands for {@link me.philcali.device.pool.Device} objects.
 */
@ApiModel
@Value.Immutable
interface CopyInputModel {
    /**
     * The source location of a copy command. If "copying to" a {@link me.philcali.device.pool.Device}, then
     * the source represents a file resource that is present on the local environment. If "copying from" a
     * {@link me.philcali.device.pool.Device}, then this value represents the file location on the
     * {@link me.philcali.device.pool.Device}.
     *
     * @return The path representing the {@link CopyInput} source
     */
    String source();

    /**
     * The destination of a copy command. If "copying to" a {@link me.philcali.device.pool.Device}, then
     * the destination represents the file location that will manifest on the {@link me.philcali.device.pool.Device}.
     * If "copying from" a {@link me.philcali.device.pool.Device}, then the destination represents a file location
     * in the local environment.
     *
     * @return The path representing the {@link CopyInput} destination
     */
    String destination();

    /**
     * Optional flags for how a copy command should work. For example, {@link CopyOption} can be provided
     * for copying directories recursively to and from {@link me.philcali.device.pool.Device}s.
     *
     * @return The distinct {@link Set} of {@link CopyOption} for this {@link CopyInput}
     */
    @Value.Default
    default Set<CopyOption> options() {
        return Collections.emptySet();
    }
}
