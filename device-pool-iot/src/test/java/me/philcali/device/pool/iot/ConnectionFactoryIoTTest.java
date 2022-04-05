/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;

@ExtendWith({MockitoExtension.class})
class ConnectionFactoryIoTTest {
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
    }
}
