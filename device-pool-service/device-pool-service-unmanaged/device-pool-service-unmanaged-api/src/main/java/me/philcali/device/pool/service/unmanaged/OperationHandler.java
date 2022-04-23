/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Singleton
public class OperationHandler {
    private static final Set<Class> SUPPORTED_CLASSES = Set.of(
            ObtainDeviceRequest.class,
            CancelReservationRequest.class
    );

    private final ObjectMapper mapper;
    private final Map<String, Function> operations;

    @Inject
    public OperationHandler(ObjectMapper mapper, Map<String, Function> operations) {
        this.operations = operations;
        this.mapper = mapper;
    }

    public void accept(String operationName, InputStream input, OutputStream output) throws IOException {
        Class requestClass = SUPPORTED_CLASSES.stream()
                .filter(rc -> rc.getSimpleName().startsWith(operationName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown operation: " + operationName));
        Function function = operations.get(operationName);
        if (Objects.isNull(function)) {
            throw new IllegalArgumentException("Unsupported operation: " + operationName);
        }
        Object request = mapper.readValue(input, requestClass);
        mapper.writeValue(output, function.apply(request));
    }
}
