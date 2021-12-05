package me.philcali.device.pool.ssh;

import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import org.apache.sshd.scp.client.ScpClient;
import org.immutables.value.Value;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@ApiModel
@Value.Immutable
abstract class ConnectionSCPModel implements ContentTransferAgent, AutoCloseable {
    abstract ScpClient client();

    private ScpClient.Option convert(CopyOption option) {
        switch (option) {
            case RECURSIVE:
                return ScpClient.Option.Recursive;
            default:
                throw new IllegalArgumentException("Do not support copy option: " + option);
        }
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
        client().getSession().close();
    }
}
