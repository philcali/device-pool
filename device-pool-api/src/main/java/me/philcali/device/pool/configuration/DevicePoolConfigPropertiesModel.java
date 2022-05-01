/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

@ApiModel
@Value.Immutable
abstract class DevicePoolConfigPropertiesModel implements DevicePoolConfig {
    private static final String DEFAULT_PROPERTIES_FILE = "devices/pool.properties";

    @Override
    @Value.Default
    public String poolClassName() {
        return DevicePoolConfigPropertiesMarshaller.DEFAULT_POOL_CLASS;
    }

    @Override
    @Value.Default
    public Map<String, DevicePoolConfigEntry> properties() {
        return Collections.emptyMap();
    }

    protected static final class DevicePoolConfigEntryProperties implements DevicePoolConfigEntry {
        private final String key;
        private final String namespace;
        private final Properties properties;
        private final SortedMap<String, DevicePoolConfigEntryProperties> children;

        DevicePoolConfigEntryProperties(String key, String namespace, Properties properties) {
            this.key = key;
            this.namespace = namespace;
            this.properties = properties;
            this.children = new TreeMap<>();
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public Optional<String> value() {
            return Optional.ofNullable(properties.getProperty(namespace));
        }

        @Override
        public Map<String, DevicePoolConfigEntry> properties() {
            return Collections.unmodifiableSortedMap(children);
        }

        Map<String, DevicePoolConfigEntryProperties> children() {
            return children;
        }
    }

    @Deprecated(forRemoval = true, since = "1.2.0")
    public static DevicePoolConfigProperties load(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "Failed to load properties; stream provided is null");
        try (InputStreamReader inputStreamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
             Reader bufferedReader = new BufferedReader(inputStreamReader)) {

            Properties properties = new Properties();
            properties.load(bufferedReader);
            return new DevicePoolConfigPropertiesMarshaller().internalLoad(properties);
        }
    }

    @Deprecated(forRemoval = true, since = "1.2.0")
    public static DevicePoolConfigProperties load(ClassLoader loader) throws IOException {
        return load(loader.getResourceAsStream(DEFAULT_PROPERTIES_FILE));
    }
}
