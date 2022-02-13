/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import me.philcali.device.pool.exceptions.DeviceInteractionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;

/**
 * The main abstraction for interacting with a {@link Device} of any kind. The
 * {@link Device} interface represent the data plane interface for interacting
 * with {@link Device}s.
 */
public interface Device extends AutoCloseable {
    /**
     * The unique identifier for this {@link Device}.
     *
     * @return the device identifier
     */
    String id();

    /**
     * Sends an arbitrary command to be executed on the {@link Device}.
     *
     * @param input The arbitrary command in the form of a {@link CommandInput}
     * @return The result of the command in the form of a {@link CommandOutput}
     * @throws DeviceInteractionException Failure to interact with the {@link Device}
     */
    CommandOutput execute(CommandInput input) throws DeviceInteractionException;

    /**
     * Attempts to send a file from this local machine to this {@link Device}.
     *
     * @param input The request to send file objects in the form of a {@link CopyInput}
     * @throws DeviceInteractionException Failure to send files to the {@link Device}
     */
    void copyTo(CopyInput input) throws DeviceInteractionException;

    /**
     * Attempts to receive a file from this {@link Device} to this local machine.
     *
     * @param input The request to receive file objects in the form of a {@link CopyInput}
     * @throws DeviceInteractionException Failure to receive files from this {@link Device}
     */
    void copyFrom(CopyInput input) throws DeviceInteractionException;

    @Override
    default void close() {
        // no-op
    }
}
