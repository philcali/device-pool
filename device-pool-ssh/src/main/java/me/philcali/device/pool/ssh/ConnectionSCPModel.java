/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssh;

import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.scp.client.ScpClient;
import org.immutables.value.Value;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A {@link ContentTransferAgent} driven by an {@link ScpClient}, which reuses the same
 * {@link org.apache.sshd.client.SshClient} that drives {@link me.philcali.device.pool.connection.Connection}
 * information.
 */
@ApiModel
@Value.Immutable
abstract class ConnectionSCPModel implements ContentTransferAgent {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionSCP.class);

    abstract ScpClient client();

    abstract boolean reusingConnection();

    private ScpClient.Option convert(CopyOption option) {
        if (option == CopyOption.RECURSIVE) {
            return ScpClient.Option.Recursive;
        }
        throw new IllegalArgumentException("Do not support copy option: " + option);
    }

    private ScpClient.Option[] convertOptions(Collection<CopyOption> options) {
        return options.stream()
                .map(this::convert)
                .collect(Collectors.toList())
                .toArray(new ScpClient.Option[] { });
    }

    @Override
    public void send(CopyInput input) throws ContentTransferException {
        try {
            client().upload(input.source(), input.destination(), convertOptions(input.options()));
        } catch (IOException e) {
            throw new ContentTransferException(e);
        }
    }

    @Override
    public void receive(CopyInput input) throws ContentTransferException {
        try {
            client().download(input.source(), input.destination(), convertOptions(input.options()));
        } catch (IOException e) {
            throw new ContentTransferException(e);
        }
    }

    @Override
    public void close() throws Exception {
        LOGGER.debug("Close is called on SCP content agent");
        if (!reusingConnection()) {
            client().getSession().close();
            LOGGER.info("Closed created session for SCP");
        }
    }
}
