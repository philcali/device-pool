/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.SsmClient;

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

        Connection connection = factory.connect(expectedHost);

        Connection expectedConnection = ConnectionSSM.builder()
                .documentName(factory.hostDocument().apply(expectedHost))
                .host(expectedHost)
                .ssm(ssm)
                .waiter(factory.waiter())
                .build();

        assertEquals(expectedConnection, connection);
    }

}
