package me.philcali.device.pool.connection;

import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.Host;

public interface ConnectionFactory extends AutoCloseable {
    Connection connect(Host host) throws ConnectionException;

    @Override
    default void close() throws Exception {
        // no-op
    }
}
