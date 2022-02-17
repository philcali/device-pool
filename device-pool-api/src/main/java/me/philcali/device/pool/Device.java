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
 * The client side abstraction for interacting with a {@link me.philcali.device.pool.Device} of any kind. The
 * {@link me.philcali.device.pool.Device} interface represent the data plane interface for interacting
 * with {@link me.philcali.device.pool.Device}s.
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface Device extends AutoCloseable {
    /**
     * The unique identifier for this {@link me.philcali.device.pool.Device}.
     *
     * @return the device identifier
     */
    String id();

    /**
     * Sends an arbitrary command to be executed on the {@link me.philcali.device.pool.Device}.
     *
     * @param input The arbitrary command in the form of a {@link me.philcali.device.pool.model.CommandInput}
     * @return The result of the command in the form of a {@link me.philcali.device.pool.model.CommandOutput}
     * @throws me.philcali.device.pool.exceptions.DeviceInteractionException Failure to interact with the {@link me.philcali.device.pool.Device}
     */
    CommandOutput execute(CommandInput input) throws DeviceInteractionException;

    /**
     * Attempts to send a file from this local machine to this {@link me.philcali.device.pool.Device}.
     *
     * @param input The request to send file objects in the form of a {@link me.philcali.device.pool.model.CopyInput}
     * @throws me.philcali.device.pool.exceptions.DeviceInteractionException Failure to send files to the {@link me.philcali.device.pool.Device}
     */
    void copyTo(CopyInput input) throws DeviceInteractionException;

    /**
     * Attempts to receive a file from this {@link me.philcali.device.pool.Device} to this local machine.
     *
     * @param input The request to receive file objects in the form of a {@link me.philcali.device.pool.model.CopyInput}
     * @throws me.philcali.device.pool.exceptions.DeviceInteractionException Failure to receive files from this {@link me.philcali.device.pool.Device}
     */
    void copyFrom(CopyInput input) throws DeviceInteractionException;

    /** {@inheritDoc} */
    @Override
    default void close() {
        // no-op
    }
}
