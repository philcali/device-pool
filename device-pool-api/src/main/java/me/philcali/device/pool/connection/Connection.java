/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.connection;

import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;

/**
 * A connection is the smallest injection point for {@link me.philcali.device.pool.Device}::execute.
 * A {@link Connection} is created from a {@link ConnectionFactory}. This component facilities
 * {@link me.philcali.device.pool.Device} interaction through custom data planes. An example could be
 * directly through EC2 or asynchronous SSM documents.
 */
public interface Connection extends AutoCloseable {
    /**
     * Any arbitrary execution through a {@link me.philcali.device.pool.Device} connection.
     *
     * @param input The command input in the form of a {@link CommandInput}
     * @return The command output in the form of a {@link CommandOutput}
     * @throws ConnectionException Failure to interact with this {@link Connection}
     */
    CommandOutput execute(CommandInput input) throws ConnectionException;

    @Override
    default void close() throws Exception {
        // no-op
    }
}
