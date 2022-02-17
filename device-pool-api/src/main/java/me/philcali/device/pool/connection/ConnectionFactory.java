/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.connection;

import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.Host;

/**
 * <p>ConnectionFactory interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface ConnectionFactory extends AutoCloseable {
    /**
     * Creates a new {@link me.philcali.device.pool.connection.Connection} for a {@link me.philcali.device.pool.Device} represented
     * by a {@link me.philcali.device.pool.model.Host} metadata. While {@link me.philcali.device.pool.model.Host} information is not strictly necessary for a
     * {@link me.philcali.device.pool.Device} implementation, a {@link me.philcali.device.pool.connection.Connection} is an injection
     * point for {@link me.philcali.device.pool.Device} interaction. This means that {@link me.philcali.device.pool.connection.Connection}
     * are primarily overridden when using a generic {@link me.philcali.device.pool.BaseDevice}.
     *
     * @param host The {@link me.philcali.device.pool.Device} details in the form of a {@link me.philcali.device.pool.model.Host}
     * @return a complete {@link me.philcali.device.pool.connection.Connection} instance capable of interacting with a {@link me.philcali.device.pool.Device}
     * @throws me.philcali.device.pool.exceptions.ConnectionException Failure to create a new {@link me.philcali.device.pool.connection.Connection} for any reason
     */
    Connection connect(Host host) throws ConnectionException;
}
