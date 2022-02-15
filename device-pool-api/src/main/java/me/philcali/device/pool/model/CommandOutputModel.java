/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import me.philcali.device.pool.exceptions.ConnectionException;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * The response model representing the output of an arbitrary command executed on a
 * {@link me.philcali.device.pool.Device}.
 */
@ApiModel
@Value.Immutable
interface CommandOutputModel {
    /**
     * If present, the standard output from executed commands, as one would expect in a shell environment.
     *
     * @return The array of bytes from standard output
     */
    @Nullable
    byte[] stdout();

    /**
     * If present, the standard error from executed commands, as one would expect in a shell environment.
     *
     * @return The array of bytes from standard error
     */
    @Nullable
    byte[] stderr();

    /**
     * If present, the original {@link CommandInput} associated to this execution output.
     *
     * @return The original {@link CommandInput} belonging to this output
     */
    @Nullable
    CommandInput originalInput();

    /**
     * Resulting exit code as one would expect in a shell environment.
     *
     * @return An integer exit code
     */
    int exitCode();

    /**
     * Convenience method to attempt to get output, but throws if the {@link CommandOutput} contains
     * error and error exit code.
     *
     * @return The byte array from stdout or thrown {@link ConnectionException} on error
     * @throws ConnectionException Thrown if the {@link CommandOutput} represents an error
     */
    default byte[] toByteArray() throws ConnectionException {
        if (exitCode() != 0) {
            throw new ConnectionException(
                    exitCode(),
                    new String(stderr(), StandardCharsets.UTF_8),
                    originalInput());
        }
        return stdout();
    }

    /**
     * Convenience method to return stdout as a UTF-8 encoded {@link String}.
     *
     * @return The stdout as {@link String}
     * @throws ConnectionException Thrown if the {@link CommandOutput} represents an error
     */
    default String toUTF8String() throws ConnectionException {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }
}
