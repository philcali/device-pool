/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.configuration;

import me.philcali.device.pool.exceptions.DevicePoolConfigMarshallException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public interface DevicePoolConfigMarshaller {
    Set<String> contentTypes();

    void marshall(OutputStream output, DevicePoolConfig config) throws DevicePoolConfigMarshallException;

    DevicePoolConfig unmarshall(InputStream stream) throws DevicePoolConfigMarshallException;

    default byte[] marshall(DevicePoolConfig config) throws DevicePoolConfigMarshallException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            marshall(outputStream, config);
            return outputStream.toByteArray();
        } catch (IOException ie) {
            throw new DevicePoolConfigMarshallException(ie);
        }
    }

    default String marshallToUTF8String(DevicePoolConfig config) throws DevicePoolConfigMarshallException {
        return new String(marshall(config), StandardCharsets.UTF_8);
    }

    default DevicePoolConfig unmarshall(byte[] bytes) throws DevicePoolConfigMarshallException {
        try (InputStream input = new ByteArrayInputStream(bytes)) {
            return unmarshall(input);
        } catch (IOException ie) {
            throw new DevicePoolConfigMarshallException(ie);
        }
    }

    default DevicePoolConfig unmarshall(String value) throws DevicePoolConfigMarshallException {
        return unmarshall(value.getBytes(StandardCharsets.UTF_8));
    }
}
