/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.content;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.Host;

/**
 * The factory for creating {@link me.philcali.device.pool.content.ContentTransferAgent} tied to the underlying
 * {@link me.philcali.device.pool.model.Host} associated to a {@link me.philcali.device.pool.Device}.
 * Not all {@link me.philcali.device.pool.Device} or concrete agents behind the
 * data plane may be able to send files, but command execution is a very basic
 * requirement, and is therefore needed for file transfer.
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface ContentTransferAgentFactory extends AutoCloseable {
    /**
     * Creates a new {@link me.philcali.device.pool.content.ContentTransferAgent} tied to the {@link me.philcali.device.pool.model.Host} representation of a
     * {@link me.philcali.device.pool.Device} abstraction.
     *
     * @param provisionId The unique identifier associated to a provision request
     * @param connection The {@link me.philcali.device.pool.connection.Connection} tied to the {@link me.philcali.device.pool.Device} abstraction
     * @param host The underlying {@link me.philcali.device.pool.model.Host} details, or data path for this file transfer
     * @return A concrete {@link me.philcali.device.pool.content.ContentTransferAgent} instance
     * @throws me.philcali.device.pool.exceptions.ContentTransferException Failure to create new {@link me.philcali.device.pool.content.ContentTransferAgent}
     */
    ContentTransferAgent connect(String provisionId, Connection connection, Host host) throws ContentTransferException;
}
