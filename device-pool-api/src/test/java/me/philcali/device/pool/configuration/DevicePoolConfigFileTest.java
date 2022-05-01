/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.exceptions.DevicePoolConfigMarshallException;
import me.philcali.device.pool.local.LocalDevicePool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith({MockitoExtension.class})
class DevicePoolConfigFileTest {
    DevicePoolConfigFile configFile;
    Path configPath = Paths.get("test.properties");

    @Mock
    DevicePoolConfigMarshaller marshaller;

    @BeforeEach
    void setUp() {
        configFile = DevicePoolConfigFile.create();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(configPath);
    }

    @Test
    void GIVEN_config_file_WHEN_load_default_that_does_not_exist_THEN_errors() {
        doReturn(Set.of("json")).when(marshaller).contentTypes();
        configFile = new DevicePoolConfigFile(List.of(marshaller));
        assertThrows(IllegalStateException.class, () -> configFile.loadDefault(ClassLoader.getSystemClassLoader()));
    }

    @Test
    void GIVEN_config_file_WHEN_loading_resources_THEN_reads_from_classpath() {
        DevicePoolConfig config = configFile.loadFromResources("devices/bad.properties");
        assertEquals("me.philcali.device.pool.DoesNotExistPool", config.poolClassName());
    }

    @Test
    void GIVEN_config_file_WHEN_loading_missing_THEN_errors() {
        assertThrows(DevicePoolConfigMarshallException.class, () -> configFile.load(Paths.get("devices/does/not/exist.properties")));
    }

    @Test
    void GIVEN_config_file_WHEN_storing_fails_THEN_errors() {
        DevicePoolConfig config = BaseDevicePoolConfig.builder().build();
        assertThrows(DevicePoolConfigMarshallException.class, () -> configFile.store(Paths.get("devices/does/not/exist.properties"), config));
    }

    @Test
    void GIVEN_config_file_WHEN_storing_THEN_loading_works() {
        DevicePoolConfig config = BaseDevicePoolConfig.builder().poolClassName(LocalDevicePool.class.getName()).build();
        configFile.store(configPath, config);
        assertTrue(Files.exists(configPath), configPath + " did not exist!");
        assertEquals(config.poolClassName(), configFile.load(configPath).poolClassName());
    }
}
