/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.connection;

import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.Host;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoopConnectionFactory implements ConnectionFactory, ContentTransferAgentFactory {
    @Override
    public Connection connect(Host host) throws ConnectionException {
        return null;
    }

    @Override
    public ContentTransferAgent connect(String provisionId, Connection connection, Host host)
            throws ContentTransferException {
        return null;
    }

    @Override
    public void close() throws Exception {
        // no-op
    }

    public static final class Builder implements ConfigBuilder<NoopConnectionFactory> {
        @Override
        public NoopConnectionFactory fromConfig(DevicePoolConfig config) {
            Optional<DevicePoolConfig.DevicePoolConfigEntry> entry = config.namespace("connection.noop");
            assertTrue(entry.isPresent(), "connection.noop namespace not exist");
            assertEquals(Optional.of("value"), entry.flatMap(e -> e.get("test")));
            return new NoopConnectionFactory();
        }
    }
}
