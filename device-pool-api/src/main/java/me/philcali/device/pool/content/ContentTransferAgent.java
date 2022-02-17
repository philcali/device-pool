/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.content;

import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.CopyInput;

/**
 * The {@link me.philcali.device.pool.content.ContentTransferAgent} is the extension point for data plane related
 * file transfer operations in {@link me.philcali.device.pool.Device} abstractions.
 * It is entirely possible that the implementation to send commands is independent of
 * any for of file transfer, and so this extension point is necessary.
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface ContentTransferAgent extends AutoCloseable {

    /**
     * Attempts to send a file through this {@link me.philcali.device.pool.content.ContentTransferAgent} instance.
     *
     * @param input A transfer request in the form of a {@link me.philcali.device.pool.model.CopyInput}
     * @throws me.philcali.device.pool.exceptions.ContentTransferException Failure to send a file through this {@link me.philcali.device.pool.content.ContentTransferAgent}
     */
    void send(CopyInput input) throws ContentTransferException;

    /**
     * Attempts to receive a file through this {@link me.philcali.device.pool.content.ContentTransferAgent} instance.
     *
     * @param input A transfer request in the form of a {@link me.philcali.device.pool.model.CopyInput}
     * @throws me.philcali.device.pool.exceptions.ContentTransferException Failure to receive a file through this {@link me.philcali.device.pool.content.ContentTransferAgent}
     */
    void receive(CopyInput input) throws ContentTransferException;

    /** {@inheritDoc} */
    @Override
    default void close() throws Exception {
        // no-op
    }
}
