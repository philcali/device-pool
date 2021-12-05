package me.philcali.device.pool.connection;

import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;

public interface Connection extends AutoCloseable {
    CommandOutput execute(CommandInput input) throws ConnectionException;
}
