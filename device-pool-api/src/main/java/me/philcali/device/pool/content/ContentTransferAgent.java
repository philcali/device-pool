/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.content;

import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.CopyInput;

public interface ContentTransferAgent extends AutoCloseable {
    void send(CopyInput input) throws ContentTransferException;

    void receive(CopyInput input) throws ContentTransferException;

    @Override
    default void close() throws Exception {
        // no-op
    }
}
