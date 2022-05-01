/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.exceptions.DevicePoolConfigMarshallException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class DevicePoolConfigFile {
    private static final String DEFAULT_PREFIX = "devices/pool";
    private final List<DevicePoolConfigMarshaller> services;

    public DevicePoolConfigFile(final List<DevicePoolConfigMarshaller> services) {
        this.services = services;
    }

    public static DevicePoolConfigFile create(ClassLoader ldr) {
        ServiceLoader<DevicePoolConfigMarshaller> services = ServiceLoader.load(DevicePoolConfigMarshaller.class, ldr);
        return new DevicePoolConfigFile(services.stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toList()));
    }

    public static DevicePoolConfigFile create() {
        return create(ClassLoader.getSystemClassLoader());
    }

    private String hintFromPath(String fileName) {
        String[] parts = fileName.split("\\.(?=[^\\.]+$)");
        return parts[parts.length - 1].toLowerCase();
    }

    private DevicePoolConfigMarshaller locateDesiredMarshaller(String hint) {
        return services.stream()
                .filter(service -> service.contentTypes().contains(hint))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No appropriate marshaller for " + hint));
    }

    private DevicePoolConfig internalLoad(String hint, InputStream inputStream) throws IOException {
        DevicePoolConfigMarshaller service = locateDesiredMarshaller(hint);
        return service.unmarshall(inputStream);
    }

    public DevicePoolConfig load(String hint, Path file) throws DevicePoolConfigMarshallException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return internalLoad(hint, inputStream);
        } catch (IOException ie) {
            throw new DevicePoolConfigMarshallException(ie);
        }
    }

    public DevicePoolConfig loadFromResources(String resourceFile) {
        final URL resource = getClass().getClassLoader().getResource(resourceFile);
        try (InputStream inputStream = Objects.requireNonNull(resource).openStream()) {
            return internalLoad(hintFromPath(resourceFile), inputStream);
        } catch (IOException ie) {
            throw new DevicePoolConfigMarshallException(ie);
        }
    }

    public DevicePoolConfig loadDefault(ClassLoader loader) {
        for (DevicePoolConfigMarshaller service : services) {
            for (String postfix : service.contentTypes()) {
                final String resourceName = DEFAULT_PREFIX + "." + postfix;
                URL resource = loader.getResource(resourceName);
                if (Objects.isNull(resource)) {
                    continue;
                }
                try (InputStream inputStream = resource.openStream()) {
                    return service.unmarshall(inputStream);
                } catch (IOException ie) {
                    throw new DevicePoolConfigMarshallException(ie);
                }
            }
        }
        throw new IllegalStateException("Failed to find [" + services.stream()
                .flatMap(s -> s.contentTypes().stream())
                .map(type -> DEFAULT_PREFIX + "." + type)
                .collect(Collectors.joining(", ")) + "] on the classpath!");
    }

    public DevicePoolConfig load(Path file) {
        return load(hintFromPath(file.getFileName().toString()), file);
    }

    public void store(Path file, DevicePoolConfig config) throws DevicePoolConfigMarshallException {
        store(hintFromPath(file.getFileName().toString()), file, config);
    }

    public void store(String hint, Path path, DevicePoolConfig config) throws DevicePoolConfigMarshallException {
        DevicePoolConfigMarshaller service = locateDesiredMarshaller(hint);
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            service.marshall(outputStream, config);
        } catch (IOException ie) {
            throw new DevicePoolConfigMarshallException(ie);
        }
    }
}
