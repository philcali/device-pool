/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

/**
 * Input contract for {@link me.philcali.device.pool.Device} interaction. The
 * resulting {@link CommandInput} represents any arbitrary command to execute
 * on the {@link me.philcali.device.pool.Device}
 */
@ApiModel
@Value.Immutable
interface CommandInputModel {
    /**
     * Optional input, in bytes, to what would represent "stdin" in shell environments.
     *
     * @return The array of bytes to as command input
     */
    @Nullable
    byte[] input();

    /**
     * The command line as a {@link String}, to what would represent as an executable
     * in shell environments.
     *
     * @return The command line executable as a {@link String}
     */
    String line();

    /**
     * Optional arguments to the executable, to what would represent as a space delimited
     * array of arguments in a shell based environment.
     *
     * @return The array of arguments to a command
     */
    @Nullable
    List<String> args();

    /**
     * Informs the data plane agent to time out the execution within this duration.
     * Note: the default is 30 seconds
     *
     * @return The {@link Duration} of time the command is allowed to run
     */
    @Value.Default
    default Duration timeout() {
        return Duration.ofSeconds(30);
    }
}
