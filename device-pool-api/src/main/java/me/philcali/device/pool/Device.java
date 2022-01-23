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

public interface Device extends AutoCloseable {
    String id();

    CommandOutput execute(CommandInput input) throws DeviceInteractionException;

    void copyTo(CopyInput input) throws DeviceInteractionException;

    void copyFrom(CopyInput input) throws DeviceInteractionException;

    @Override
    default void close() {
        // no-op
    }
}
