/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.unmanaged.operation.OperationFunction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

@Singleton
public class OperationHandler {
    private final ObjectMapper mapper;
    private final Map<String, OperationFunction> operations;

    @Inject
    public OperationHandler(ObjectMapper mapper, Map<String, OperationFunction> operations) {
        this.mapper = mapper;
        this.operations = operations;
    }

    public void accept(String operationName, InputStream input, OutputStream output) throws IOException {
        OperationFunction function = operations.get(operationName);
        if (Objects.isNull(function)) {
            throw new IllegalArgumentException("Unknown operation " + operationName);
        }
        Object value = mapper.readValue(input, function.inputType());
        mapper.writeValue(output, function.apply(value));
    }
}
