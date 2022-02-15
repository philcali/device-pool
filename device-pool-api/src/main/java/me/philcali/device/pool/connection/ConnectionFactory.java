/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.connection;

import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.Host;

public interface ConnectionFactory extends AutoCloseable {
    /**
     * Creates a new {@link Connection} for a {@link me.philcali.device.pool.Device} represented
     * by a {@link Host} metadata. While {@link Host} information is not strictly necessary for a
     * {@link me.philcali.device.pool.Device} implementation, a {@link Connection} is an injection
     * point for {@link me.philcali.device.pool.Device} interaction. This means that {@link Connection}
     * are primarily overridden when using a generic {@link me.philcali.device.pool.BaseDevice}.
     *
     * @param host The {@link me.philcali.device.pool.Device} details in the form of a {@link Host}
     * @return a complete {@link Connection} instance capable of interacting with a {@link me.philcali.device.pool.Device}
     * @throws ConnectionException Failure to create a new {@link Connection} for any reason
     */
    Connection connect(Host host) throws ConnectionException;
}
