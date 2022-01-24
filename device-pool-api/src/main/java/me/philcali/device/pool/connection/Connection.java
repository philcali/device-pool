/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.connection;

import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;

public interface Connection extends AutoCloseable {
    CommandOutput execute(CommandInput input) throws ConnectionException;

    @Override
    default void close() throws Exception {
        // no-op
    }
}
