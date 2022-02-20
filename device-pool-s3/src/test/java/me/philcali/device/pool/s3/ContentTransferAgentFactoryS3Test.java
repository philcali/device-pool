/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class ContentTransferAgentFactoryS3Test {
    @Mock
    private Connection connection;
    @Mock
    private S3Client s3;
    private Host host;
    private ContentTransferAgentFactory factory;
    private final String bucketName = "testBucket";

    @BeforeEach
    void setup() {
        host = Host.builder()
                .hostName("myhost.com")
                .deviceId("i-abcabcabc")
                .platform(PlatformOS.of("Linux", "armv7"))
                .port(22)
                .build();
        factory = ContentTransferAgentFactoryS3.builder()
                .bucketName(bucketName)
                .s3(s3)
                .build();
    }

    @Test
    void GIVEN_factory_is_created_WHEN_connecting_to_a_host_THEN_agent_is_returned() throws Exception {
        ContentTransferAgent agent = factory.connect("abc-123", connection, host);

        ContentTransferAgent expectedAgent = ContentTransferAgentS3.builder()
                .bucketName(bucketName)
                .prefix("abc-123/i-abcabcabc")
                .command(AWSCLIAgentCommand.create())
                .s3(s3)
                .connection(connection)
                .build();

        assertEquals(expectedAgent, agent);

        factory.close();
        verify(s3).close();
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_used_THEN_factory_is_created() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader());
        ContentTransferAgentFactoryS3 factoryS3 = ContentTransferAgentFactoryS3.builder().s3(s3).fromConfig(config);
        assertEquals("mybucket", factoryS3.bucketName());
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_missing_THEN_exception_is_thrown() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("devices/pool.properties"));
        properties.clear();
        DevicePoolConfig config = DevicePoolConfigProperties.load(properties);
        assertThrows(ContentTransferException.class, () -> ContentTransferAgentFactoryS3.builder()
                .s3(s3)
                .fromConfig(config));
    }
}
