/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.BaseDevicePool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DevicePoolConfigPropertiesMarshallerTest {
    DevicePoolConfigPropertiesMarshaller marshaller;

    DevicePoolConfig testConfig;

    @BeforeEach
    void setUp() {
        marshaller = new DevicePoolConfigPropertiesMarshaller();
        testConfig = BaseDevicePoolConfig.builder()
                .poolClassName(BaseDevicePool.class.getName())
                .addEntry(BaseDevicePoolConfigEntry.builder()
                        .key("provision")
                        .addEntry(BaseDevicePoolConfigEntry.builder()
                                .key("lab")
                                .addEntry(BaseDevicePoolConfigEntry.builder().key("endpoint").value("example.com").build())
                                .addEntry(BaseDevicePoolConfigEntry.builder().key("port").value("22").build())
                                .build())
                        .build())
                .build();
    }

    @Test
    void GIVEN_marshaller_WHEN_marshalling_to_string_THEN_unmarshall_from_string_is_possible() {
        String propertiesContents = marshaller.marshallToUTF8String(testConfig);
        String expectedContent = "device.pool.provision.lab.endpoint=example.com\n"
                + "device.pool.provision.lab.port=22\n"
                + "device.pool.class=me.philcali.device.pool.BaseDevicePool";
        // Skip the generated comments with timestamp
        assertEquals(expectedContent, Arrays.stream(propertiesContents.split("\n")).skip(2)
                .collect(Collectors.joining("\n")));
        DevicePoolConfig propertiesConfig = marshaller.unmarshall(propertiesContents);
        assertEquals(testConfig.poolClassName(), propertiesConfig.poolClassName());
        assertEquals(testConfig.namespace("provision.lab").get().properties().keySet(), propertiesConfig.namespace("provision.lab").get().properties().keySet());
    }
}
