package me.philcali.device.pool;

import me.philcali.device.pool.exceptions.DeviceInteractionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;

public interface Device {
    String id();

    CommandOutput execute(CommandInput input) throws DeviceInteractionException;

    void copyTo(CopyInput input) throws DeviceInteractionException;

    void copyFrom(CopyInput input) throws DeviceInteractionException;
}
