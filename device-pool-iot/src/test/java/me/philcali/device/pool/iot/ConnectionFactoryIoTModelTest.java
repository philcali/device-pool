/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class ConnectionFactoryIoTModelTest {
    @Mock
    MqttClientConnection connection;

    ConnectionFactoryIoT factory;

    @BeforeEach
    void setUp() {
        factory = ConnectionFactoryIoT.builder()
                .connection(connection)
                .build();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connect_to_host_THEN_subscription_is_made() {
        Host host = Host.builder()
                .hostName("example.com")
                .deviceId("example")
                .platform(PlatformOS.of("Linux", "armv6"))
                .build();
        final CompletableFuture<Integer> subscribe = new CompletableFuture<>();
        subscribe.complete(0);
        doReturn(subscribe).when(connection).subscribe(
                eq("command/example/execute/result"),
                eq(QualityOfService.AT_LEAST_ONCE),
                any(Consumer.class));
        factory.connect(host);
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connect_to_host_THEN_exception_is_thrown()
            throws ExecutionException, InterruptedException {
        Host host = Host.builder()
                .hostName("example.com")
                .deviceId("example")
                .platform(PlatformOS.of("Linux", "armv6"))
                .build();
        CompletableFuture<Integer> subscribe = mock(CompletableFuture.class);
        doReturn(subscribe).when(connection).subscribe(
                eq("command/example/execute/result"),
                eq(QualityOfService.AT_LEAST_ONCE),
                any(Consumer.class));
        doThrow(ExecutionException.class).when(subscribe).get();
        assertThrows(ConnectionException.class, () -> factory.connect(host));
    }

    @Test
    void GIVEN_factory_is_created_WHEN_closing_THEN_closing_connection_is_invoked() throws Exception {
        final CompletableFuture<Void> disconnect = new CompletableFuture<>();
        disconnect.complete(null);
        doReturn(disconnect).when(connection).disconnect();
        factory.close();
        verify(connection).close();
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_used_THEN_factory_is_created() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader());
        assertThrows(ConnectionException.class, () -> ConnectionFactoryIoT.builder().from(factory).fromConfig(config));
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_default_THEN_default_is_used() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("devices/pool.properties"));
        properties.clear();
        DevicePoolConfig config = DevicePoolConfigProperties.load(properties);
        assertThrows(ConnectionException.class, () -> ConnectionFactoryIoT.builder().from(factory).fromConfig(config));
    }
}
