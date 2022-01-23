/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.content;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.Host;

public interface ContentTransferAgentFactory extends AutoCloseable {
    ContentTransferAgent connect(String provisionId, Connection connection, Host host) throws ContentTransferException;
}
