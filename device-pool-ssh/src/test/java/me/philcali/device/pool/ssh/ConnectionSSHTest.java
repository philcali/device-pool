/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssh;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ConnectionSSHTest {
    @Mock
    private ClientSession session;
    private Connection connection;

    @BeforeEach
    void setup() {
        connection = ConnectionSSH.of(session);
    }

    @Test
    void GIVEN_ssh_connection_WHEN_executing_THEN_sends_remote_commands() throws IOException {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello", "World")
                .input("Hello World".getBytes(StandardCharsets.UTF_8))
                .build();
        ChannelExec exec = mock(ChannelExec.class);
        OpenFuture openFuture = mock(OpenFuture.class);
        when(session.createExecChannel(eq("echo Hello World"))).thenReturn(exec);
        when(exec.waitFor(eq(ClientSession.REMOTE_COMMAND_WAIT_EVENTS), eq(input.timeout()))).thenReturn(Collections.emptySet());
        doReturn(openFuture).when(exec).open();
        doReturn(0).when(exec).getExitStatus();
        CommandOutput output = connection.execute(input);
        assertEquals(CommandOutput.builder().exitCode(0).originalInput(input).stderr().stdout().build(), output);
        verify(openFuture).await(eq(input.timeout()));
    }

    @Test
    void GIVEN_ssh_connection_WHEN_executing_times_out_THEN_wrapped_ex_is_thrown() throws IOException {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello", "World")
                .build();
        ChannelExec exec = mock(ChannelExec.class);
        OpenFuture openFuture = mock(OpenFuture.class);
        when(session.createExecChannel(eq("echo Hello World"))).thenReturn(exec);
        when(exec.waitFor(eq(ClientSession.REMOTE_COMMAND_WAIT_EVENTS), eq(input.timeout())))
                .thenReturn(Collections.singleton(ClientChannelEvent.TIMEOUT));
        doReturn(openFuture).when(exec).open();
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_ssh_connection_WHEN_closing_THEN_session_is_closed() throws Exception {
        connection.close();
        verify(session).close();
    }
}
