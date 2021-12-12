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
