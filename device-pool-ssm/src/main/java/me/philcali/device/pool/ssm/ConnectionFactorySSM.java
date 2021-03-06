/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import org.immutables.value.Value;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link me.philcali.device.pool.connection.ConnectionFactory} using AWS Simple Systems Manager (SSM) as the data plane implementation.
 * Any {@link me.philcali.device.pool.Device} capable of handling this {@link me.philcali.device.pool.connection.Connection} must run an
 * SSMAgent communicating to SSM service's data plane.
 */
@APIShadowModel
@Value.Immutable
public abstract class ConnectionFactorySSM implements ConnectionFactory {
    private static final String WINDOWS_DOCUMENT = "AWS-RunPowerShellScript";
    private static final String LINUX_DOCUMENT = "AWS-RunShellScript";

    /**
     * <p>ssm.</p>
     *
     * @return a {@link software.amazon.awssdk.services.ssm.SsmClient} object
     */
    @Value.Default
    public SsmClient ssm() {
        return SsmClient.create();
    }

    /**
     * <p>hostDocument.</p>
     *
     * @return a {@link me.philcali.device.pool.ssm.HostDocument} object
     */
    @Value.Default
    public HostDocument hostDocument() {
        return host -> host.platform().isWindows() ? WINDOWS_DOCUMENT : LINUX_DOCUMENT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ConnectionFactorySSM create() {
        return ConnectionFactorySSM.builder().build();
    }

    public static final class Builder
            extends ImmutableConnectionFactorySSM.Builder
            implements ConfigBuilder<ConnectionFactorySSM> {
        @Override
        public ConnectionFactorySSM fromConfig(DevicePoolConfig config) {
            return config.namespace("connection.ssm")
                    .map(entry -> {
                        entry.get("document").ifPresent(document -> hostDocument(host -> document));
                        return build();
                    })
                    .orElseGet(this::build);
        }
    }

    /**
     * <p>waiter.</p>
     *
     * @return a {@link software.amazon.awssdk.core.waiters.Waiter} object
     */
    @Value.Default
    public Waiter<GetCommandInvocationResponse> waiter() {
        final Set<CommandInvocationStatus> terminalStatuses = new HashSet<>(Arrays.asList(
                CommandInvocationStatus.SUCCESS,
                CommandInvocationStatus.FAILED
        ));
        return Waiter.builder(GetCommandInvocationResponse.class)
                .addAcceptor(WaiterAcceptor.errorOnResponseAcceptor(response ->
                        response.status().equals(CommandInvocationStatus.CANCELLED), "Command was cancelled!"))
                .addAcceptor(WaiterAcceptor.errorOnResponseAcceptor(response ->
                        response.status().equals(CommandInvocationStatus.TIMED_OUT), "Command timed out!"))
                .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(response ->
                        !terminalStatuses.contains(response.status())))
                .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(response ->
                        terminalStatuses.contains(response.status())))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public Connection connect(Host host) throws ConnectionException {
        return ConnectionSSM.builder()
                .ssm(ssm())
                .host(host)
                .waiter(waiter())
                .documentName(hostDocument().apply(host))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        ssm().close();
    }
}
