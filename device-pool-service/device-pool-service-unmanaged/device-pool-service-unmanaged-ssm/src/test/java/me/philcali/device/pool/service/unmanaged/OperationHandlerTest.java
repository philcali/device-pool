/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import me.philcali.device.pool.service.unmanaged.module.UnmanagedComponent;
import me.philcali.device.pool.service.unmanaged.operation.OperationFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class OperationHandlerTest {
    private OperationHandler handler;
    private Map<String, OperationFunction> functions;

    @Mock
    OperationFunction obtainDevice;

    @Mock
    UnmanagedComponent component;

    @Mock
    Context context;

    @Mock
    ClientContext clientContext;

    InputStream inputStream;

    ObtainDeviceRequest originalRequest;
    ObjectMapper mapper;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        functions = new HashMap<>();
        functions.put("ObtainDevice", obtainDevice);

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        handler = new OperationHandler(mapper, functions);

        doReturn(ObtainDeviceRequest.class).when(obtainDevice).inputType();

        originalRequest = ObtainDeviceRequest.builder()
                .accountKey(CompositeKey.of("012345678912"))
                .provision(ProvisionObject.of(Instant.now(), UUID.randomUUID().toString(), Status.PROVISIONING))
                .build();

        byte[] outputBytes = mapper.writeValueAsBytes(originalRequest);
        inputStream = new ByteArrayInputStream(outputBytes);
    }

    @Test
    void GIVEN_handler_is_created_WHEN_accept_is_invoked_THEN_value_is_returned() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class,
                () -> handler.accept("CancelReservation", inputStream, outputStream));

        ObtainDevicesSSM ssm = new ObtainDevicesSSM(component);
        doReturn(handler).when(component).handler();

        ObtainDeviceResponse expectedResponse = ObtainDeviceResponse.builder()
                .accountKey(originalRequest.accountKey())
                .status(Status.SUCCEEDED)
                .device(DeviceObject.builder()
                        .id("device-1")
                        .poolId("pool-1")
                        .updatedAt(Instant.now())
                        .build())
                .build();

        doReturn(expectedResponse).when(obtainDevice).apply(eq(originalRequest));
        doReturn(clientContext).when(context).getClientContext();
        doReturn(Map.of("operationName", "ObtainDevice")).when(clientContext).getCustom();
        ssm.handleRequest(inputStream, outputStream, context);
        assertEquals(expectedResponse, mapper.readValue(outputStream.toByteArray(), ObtainDeviceResponse.class));
    }
}
