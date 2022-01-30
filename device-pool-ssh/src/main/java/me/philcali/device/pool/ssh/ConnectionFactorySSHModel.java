/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssh;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.scp.client.DefaultScpClient;
import org.apache.sshd.scp.client.ScpClient;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@ApiModel
@Value.Immutable
abstract class ConnectionFactorySSHModel implements ConnectionFactory, ContentTransferAgentFactory {
    @Value.Default
    public SshClient client() {
        return SshClient.setUpDefaultClient();
    }

    @Nullable
    abstract List<KeyPair> publicKeys();

    @Value.Default
    public Duration connectionTimeout() {
        return Duration.ofSeconds(5L);
    }

    @Value.Default
    public Duration authTimeout() {
        return Duration.ofSeconds(5L);
    }

    @Value.Default
    public String userName() {
        return System.getProperty("user.name");
    }

    @Value.Default
    Function<ClientSession, ScpClient> scpFactory() {
        return DefaultScpClient::new;
    }

    @Nullable
    abstract AttributeRepository attributeRepository();

    @Nullable
    abstract SocketAddress localAddress();

    public static ConnectionFactorySSH create() {
        return ConnectionFactorySSH.builder().build();
    }

    private SshClient forcedStartClient() {
        if (!client().isStarted()) {
            synchronized (client()) {
                if (!client().isStarted()) {
                    Optional.ofNullable(publicKeys()).ifPresent(keys -> keys.forEach(client()::addPublicKeyIdentity));
                    client().start();
                }
            }
        }
        return client();
    }

    private ClientSession doConnect(Host host) throws IOException {
        final HostConfigEntry hostConfigEntry = new HostConfigEntry();
        hostConfigEntry.setHostName(host.hostName());
        hostConfigEntry.setPort(host.port());
        hostConfigEntry.setProxyJump(host.proxyJump());
        hostConfigEntry.setUsername(userName());
        ConnectFuture future = forcedStartClient().connect(hostConfigEntry, attributeRepository(), localAddress());
        ClientSession session = future.verify(connectionTimeout()).getSession();
        session.auth().verify(connectionTimeout());
        return session;
    }

    @Override
    public Connection connect(final Host host) throws ConnectionException {
        try {
            return ConnectionSSH.of(doConnect(host));
        } catch (IOException ie) {
            throw new ConnectionException(ie);
        }
    }

    @Override
    public ContentTransferAgent connect(String id, Connection connection, Host host) throws ContentTransferException {
        try {
            ClientSession session;
            boolean reusingConnection = false;
            // Attempt to re-use any connection if possible before creating a new one
            if (connection instanceof ConnectionSSH) {
                session = ((ConnectionSSH) connection).clientSession();
                reusingConnection = true;
            } else {
                session = doConnect(host);
            }
            return scpFactory()
                    .andThen(ConnectionSCP.builder().reusingConnection(reusingConnection)::client)
                    .andThen(ConnectionSCP.Builder::build)
                    .apply(session);
        } catch (IOException ie) {
            throw new ContentTransferException(ie);
        }
    }

    @Override
    public void close() throws Exception {
        client().close();
    }
}
