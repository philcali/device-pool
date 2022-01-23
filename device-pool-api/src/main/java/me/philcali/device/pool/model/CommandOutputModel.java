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

@ApiModel
@Value.Immutable
interface CommandOutputModel {
    @Nullable
    byte[] stdout();

    @Nullable
    byte[] stderr();

    @Nullable
    CommandInput originalInput();

    int exitCode();

    default byte[] toByteArray() throws ConnectionException {
        if (exitCode() != 0) {
            throw new ConnectionException(
                    exitCode(),
                    new String(stderr(), StandardCharsets.UTF_8),
                    originalInput());
        }
        return stdout();
    }

    default String toUTF8String() throws ConnectionException {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }
}
