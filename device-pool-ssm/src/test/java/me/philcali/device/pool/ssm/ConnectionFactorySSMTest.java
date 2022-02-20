/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class ConnectionFactorySSMTest {

    private ConnectionFactorySSM factory;

    @Mock
    private SsmClient ssm;

    @BeforeEach
    void setup() {
        factory = ConnectionFactorySSM.builder()
                .ssm(ssm)
                .build();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_close_is_called_THEN_forwards_to_client() throws Exception {
        factory.close();
        verify(ssm).close();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_factory_connects_THEN_connection_is_established() {
        Host expectedHost = Host.builder()
                .port(22)
                .hostName("127.0.0.1")
                .deviceId("i-abc1234efg")
                .platform(PlatformOS.builder()
                        .os("Linux")
                        .arch("aarch64")
                        .build())
                .build();

        Host windowsHost = Host.builder()
                .port(22)
                .hostName("127.0.0.1")
                .deviceId("i-eeeeeggggfff")
                .platform(PlatformOS.builder()
                        .os("Windows")
                        .arch("amd64")
                        .build())
                .build();

        Connection connection = factory.connect(expectedHost);
        Connection windows = factory.connect(windowsHost);

        Connection expectedConnection = ConnectionSSM.builder()
                .documentName(factory.hostDocument().apply(expectedHost))
                .host(expectedHost)
                .ssm(ssm)
                .waiter(factory.waiter())
                .build();

        Connection windowsConnection = ConnectionSSM.builder()
                .documentName(factory.hostDocument().apply(windowsHost))
                .host(windowsHost)
                .ssm(ssm)
                .waiter(factory.waiter())
                .build();

        assertEquals(expectedConnection, connection);
        assertEquals(windowsConnection, windows);
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_provided_THEN_created_with_config() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader());
        ConnectionFactorySSM factorySSM = ConnectionFactorySSM.builder().ssm(ssm).fromConfig(config);
        assertEquals("RunMyShellCommand", factorySSM.hostDocument().apply(null));
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_empty_THEN_create_default() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("devices/pool.properties"));
        properties.clear();
        DevicePoolConfig config = DevicePoolConfigProperties.load(properties);
        assertEquals(factory, ConnectionFactorySSM.builder().ssm(ssm).waiter(factory.waiter()).fromConfig(config));
    }
}
