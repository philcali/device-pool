/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

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
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iot.model.DescribeEndpointResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class ConnectionFactoryShadowTest {
    private ConnectionFactoryShadow factory;

    @Mock
    IotDataPlaneClient dataPlaneClient;

    @Mock
    IotClient controlPlaneClient;

    @BeforeEach
    void setUp() {
        factory = ConnectionFactoryShadow.builder()
                .shadowName("shadow")
                .dataPlaneClient(dataPlaneClient)
                .build();
    }

    @Test
    void GIVEN_iot_control_plane_WHEN_describing_endpoint_THEN_endpoint_is_provided() {
        DescribeEndpointResponse response = DescribeEndpointResponse.builder()
                .endpointAddress("https://example.com")
                .build();
        doReturn(response).when(controlPlaneClient).describeEndpoint(eq(DescribeEndpointRequest.builder()
                .endpointType("iot:Data-ATS")
                .build()));
        assertEquals("https://example.com", ConnectionFactoryShadow.describeDataEndpoint(controlPlaneClient));
    }

    @Test
    void GIVEN_factory_is_created_WHEN_factory_is_closing_THEN_client_is_closed() {
        factory.close();
        verify(dataPlaneClient).close();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_factory_connects_to_host_THEN_connection_is_provided() {
        Connection connection = factory.connect(Host.builder()
                .deviceId("abc-123")
                .hostName("example.com")
                .platform(PlatformOS.of("Linux", "armv6"))
                .port(22)
                .build());
        assertInstanceOf(ConnectionShadow.class, connection);
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_used_THEN_factory_is_created() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader());
        assertNotNull(ConnectionFactoryShadow.builder().fromConfig(config));
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_default_THEN_default_is_used() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("devices/pool.properties"));
        properties.clear();
        DevicePoolConfig config = DevicePoolConfigProperties.load(properties);
        ConnectionFactoryShadow shadow = ConnectionFactoryShadow.builder().from(factory).fromConfig(config);
        assertEquals(factory, shadow);
    }
}
