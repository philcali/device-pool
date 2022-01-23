/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssh;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.immutables.value.Value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@ApiModel
@Value.Immutable
abstract class ConnectionSSHModel implements Connection {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionSSH.class);

    abstract ClientSession clientSession();

    @Override
    public CommandOutput execute(final CommandInput input) throws ConnectionException {
        final StringBuilder builder = new StringBuilder(input.line());
        Optional.ofNullable(input.args()).ifPresent(args -> builder.append(' ').append(String.join(" ", args)));
        LOGGER.debug("Executing command on {}: {}", clientSession().getConnectAddress(), builder);
        try (ByteArrayOutputStream error = new ByteArrayOutputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream();
                ChannelExec exec = clientSession().createExecChannel(builder.toString())) {
            if (input.input() != null) {
                exec.setIn(new ByteArrayInputStream(input.input()));
            }
            exec.setErr(error);
            exec.setOut(output);
            exec.open().await(input.timeout());
            Set<ClientChannelEvent> masks = exec.waitFor(ClientSession.REMOTE_COMMAND_WAIT_EVENTS, input.timeout());
            if (masks.contains(ClientChannelEvent.TIMEOUT)) {
                throw new ConnectionException("Connection timeout running: " + builder);
            }
            int exitCode = exec.getExitStatus();
            return CommandOutput.builder()
                    .exitCode(exitCode)
                    .stderr(error.toByteArray())
                    .stdout(output.toByteArray())
                    .originalInput(input)
                    .build();
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public void close() throws Exception {
        clientSession().close();
    }
}
