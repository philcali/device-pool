/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * The data path details {@link me.philcali.device.pool.Device} reachable information. The
 * {@link Host} is used in data plane components like {@link me.philcali.device.pool.connection.Connection}
 * and {@link me.philcali.device.pool.content.ContentTransferAgent} for executing commands and file transfer.
 */
@ApiModel
@Value.Immutable
interface HostModel {
    /**
     * The operating system and CPU architecture combo for this {@link Host}. This information should be
     * treated as a "best effort" hint for data plane implementations that need to handle operating systems
     * differently.
     *
     * @return The {@link PlatformOS} of this {@link Host}
     */
    PlatformOS platform();

    /**
     * The host name or address of this {@link me.philcali.device.pool.Device}.
     *
     * @return A {@link String} representation of a {@link Host}
     */
    String hostName();

    /**
     * The port of communication for this {@link me.philcali.device.pool.Device}. In many cases,
     * interaction will be handled directly through an SSH server, and thus the default value is
     * port 22.
     *
     * @return An integer value of a connection port
     */
    @Value.Default
    default Integer port() {
        return 22;
    }

    /**
     * Optional proxy jump information for SSH clients that understand proxy jump hosts. A proxy jump,
     * also known as a "bastion", is a measure of connection security, to block direct SSH connectivity
     * to a {@link me.philcali.device.pool.Device} behind a private network.
     *
     * @return The {@link String} representation of a proxy jump address
     */
    @Nullable
    String proxyJump();

    /**
     * The underlying {@link me.philcali.device.pool.Device} identifier.
     *
     * @return A {@link String} representation of a {@link me.philcali.device.pool.Device} identifier
     */
    String deviceId();
}
