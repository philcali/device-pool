/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssh;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ConnectionFactorySSHTest {
    @Mock
    private SshClient sshClient;
    @Mock
    private ScpClient scpClient;
    private ConnectionFactorySSH factory;

    private Host host;

    @BeforeEach
    void setup() {
        host = Host.builder()
                .hostName("myhost.amazon.com")
                .platform(PlatformOS.of("Linux", "aarch64"))
                .deviceId("my-instance-id")
                .build();

        factory = ConnectionFactorySSH.builder()
                .client(sshClient)
                .scpFactory(session -> scpClient)
                .build();
    }

    @Test
    void GIVEN_factory_WHEN_create_THEN_factory_is_provided() {
        assertNotNull(ConnectionFactorySSH.create());
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_through_ssh_THEN_ssh_connection_is_created() throws IOException {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        final ClientSession session = mock(ClientSession.class);
        final AuthFuture authFuture = mock(AuthFuture.class);
        doReturn(false).when(sshClient).isStarted();
        when(sshClient.connect(any(HostConfigEntry.class), eq(null), eq(null))).then(answer -> {
            HostConfigEntry entry = answer.getArgument(0);
            assertEquals(host.hostName(), entry.getHostName());
            assertEquals(22, entry.getPort());
            assertEquals(factory.userName(), entry.getUsername());
            return connectFuture;
        });
        when(connectFuture.verify(eq(Duration.ofSeconds(5L)))).thenReturn(connectFuture);
        doReturn(session).when(connectFuture).getSession();
        doReturn(authFuture).when(session).auth();
        when(authFuture.verify(eq(Duration.ofSeconds(5L)))).thenReturn(authFuture);
        Connection connection = factory.connect(host);
        assertEquals(ConnectionSSH.of(session), connection);
        verify(sshClient).start();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_through_ssh_THEN_ssh_client_is_reused() throws IOException {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        final ClientSession session = mock(ClientSession.class);
        final AuthFuture authFuture = mock(AuthFuture.class);
        doReturn(true).when(sshClient).isStarted();
        when(sshClient.connect(any(HostConfigEntry.class), eq(null), eq(null))).then(answer -> {
            HostConfigEntry entry = answer.getArgument(0);
            assertEquals(host.hostName(), entry.getHostName());
            assertEquals(22, entry.getPort());
            assertEquals(factory.userName(), entry.getUsername());
            return connectFuture;
        });
        when(connectFuture.verify(eq(Duration.ofSeconds(5L)))).thenReturn(connectFuture);
        doReturn(session).when(connectFuture).getSession();
        doReturn(authFuture).when(session).auth();
        when(authFuture.verify(eq(Duration.ofSeconds(5L)))).thenReturn(authFuture);
        Connection connection = factory.connect(host);
        assertEquals(ConnectionSSH.of(session), connection);
        verify(sshClient, times(0)).start();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_through_ssh_fails_THEN_wrapped_exception_is_thrown()
            throws IOException {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        doReturn(true).when(sshClient).isStarted();
        when(sshClient.connect(any(HostConfigEntry.class), eq(null), eq(null))).then(answer -> {
            HostConfigEntry entry = answer.getArgument(0);
            assertEquals(host.hostName(), entry.getHostName());
            assertEquals(22, entry.getPort());
            assertEquals(factory.userName(), entry.getUsername());
            return connectFuture;
        });
        doThrow(IOException.class).when(connectFuture).verify(eq(Duration.ofSeconds(5L)));
        assertThrows(ConnectionException.class, () -> factory.connect(host));
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_through_scp_THEN_new_connection_is_using_scp() throws IOException {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        final ClientSession session = mock(ClientSession.class);
        final AuthFuture authFuture = mock(AuthFuture.class);
        doReturn(true).when(sshClient).isStarted();
        when(sshClient.connect(any(HostConfigEntry.class), eq(null), eq(null))).then(answer -> {
            HostConfigEntry entry = answer.getArgument(0);
            assertEquals(host.hostName(), entry.getHostName());
            assertEquals(22, entry.getPort());
            assertEquals(factory.userName(), entry.getUsername());
            return connectFuture;
        });
        when(connectFuture.verify(eq(Duration.ofSeconds(5L)))).thenReturn(connectFuture);
        doReturn(session).when(connectFuture).getSession();
        doReturn(authFuture).when(session).auth();
        when(authFuture.verify(eq(Duration.ofSeconds(5L)))).thenReturn(authFuture);
        Connection oldConnection = mock(Connection.class);
        ContentTransferAgent agent = factory.connect("abc-123", oldConnection, host);
        assertEquals(ConnectionSCP.of(scpClient, false), agent);
        verify(sshClient, times(0)).start();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_through_scp_fails_THEN_wrapped_ex_is_thrown() throws IOException {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        doReturn(true).when(sshClient).isStarted();
        when(sshClient.connect(any(HostConfigEntry.class), eq(null), eq(null))).then(answer -> {
            HostConfigEntry entry = answer.getArgument(0);
            assertEquals(host.hostName(), entry.getHostName());
            assertEquals(22, entry.getPort());
            assertEquals(factory.userName(), entry.getUsername());
            return connectFuture;
        });
        doThrow(IOException.class).when(connectFuture).verify(eq(Duration.ofSeconds(5L)));
        Connection oldConnection = mock(Connection.class);
        assertThrows(ContentTransferException.class, () -> factory.connect("abc-123", oldConnection, host));
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_through_scp_THEN_connection_is_reused_for_scp() {
        final ClientSession session = mock(ClientSession.class);
        ConnectionSSH oldConnection = ConnectionSSH.of(session);
        ContentTransferAgent agent = factory.connect("abc-123", oldConnection, host);
        assertEquals(ConnectionSCP.of(scpClient, true), agent);
        verify(sshClient, times(0)).start();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_factory_is_closed_THEN_ssh_client_is_closed() throws Exception {
        factory.close();
        verify(sshClient).close();
    }
}
