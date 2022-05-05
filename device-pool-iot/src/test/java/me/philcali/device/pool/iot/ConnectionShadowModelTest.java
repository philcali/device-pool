/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.iot.iotshadow.model.GetShadowResponse;
import software.amazon.awssdk.iot.iotshadow.model.ShadowStateWithDelta;
import software.amazon.awssdk.iot.iotshadow.model.UpdateShadowRequest;
import software.amazon.awssdk.iot.iotshadow.model.UpdateShadowResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.IotDataPlaneException;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class})
class ConnectionShadowModelTest {
    @Mock
    private IotDataPlaneClient dataPlaneClient;

    private ConnectionShadow connection;

    private CommandInput input = CommandInput.builder()
            .line("echo")
            .addArgs("Hello", "World")
            .timeout(Duration.ofSeconds(5))
            .input("Hello World".getBytes(StandardCharsets.UTF_8))
            .build();

    @BeforeEach
    void setUp() {
        connection = ConnectionShadow.builder()
                .dataPlaneClient(dataPlaneClient)
                .shadowName("shadowName")
                .mapper(new ObjectMapper())
                .host(Host.of(PlatformOS.of("Linux", "armv6"), "abc-123", "example.com"))
                .build();
    }

    @Test
    void GIVEN_connection_created_WHEN_shadow_is_empty_THEN_upsert_document() throws JsonProcessingException {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicInteger getCalls = new AtomicInteger();
        when(dataPlaneClient.getThingShadow(eq(getRequest))).then(answer -> {
            GetThingShadowResponse.Builder builder = GetThingShadowResponse.builder();
            if (getCalls.getAndIncrement() == 0) {
                throw ResourceNotFoundException.builder().build();
            } else if (getCalls.getAndIncrement() == 1) {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 1;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, currentId.get());
                }};
                response.state.desired = new HashMap<>(response.state.reported);
                builder.payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)));
            } else {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 2;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, currentId.get());
                    put("output", new HashMap<>() {{
                        put(ConnectionIoT.FIELD_EXIT_CODE, 0);
                        put(ConnectionIoT.FIELD_STDOUT, Base64.getEncoder().encodeToString("Hello World".getBytes(StandardCharsets.UTF_8)));
                        put(ConnectionIoT.FIELD_STDERR, Base64.getEncoder().encodeToString("command 'echo' is not found".getBytes(StandardCharsets.UTF_8)));
                    }});
                }};
                response.state.desired = new HashMap<>(response.state.reported);
                builder.payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)));
            }
            return builder.build();
        });
        UpdateShadowResponse shadowResponse = new UpdateShadowResponse();
        shadowResponse.version = 1;
        UpdateThingShadowResponse updateResponse = UpdateThingShadowResponse.builder()
                .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(shadowResponse)))
                .build();
        when(dataPlaneClient.updateThingShadow(any(UpdateThingShadowRequest.class))).then(answer -> {
            UpdateThingShadowRequest request = answer.getArgument(0);
            UpdateShadowRequest payload = connection.mapper().readValue(
                    request.payload().asByteArray(), UpdateShadowRequest.class);
            assertTrue(payload.state.desired.containsKey(ConnectionIoT.FIELD_ID));
            currentId.set(payload.state.desired.get(ConnectionIoT.FIELD_ID).toString());
            return updateResponse;
        });
        CommandOutput output = connection.execute(input);
        CommandOutput expected = CommandOutput.builder()
                .exitCode(0)
                .originalInput(input)
                .stdout("Hello World".getBytes(StandardCharsets.UTF_8))
                .stderr("command 'echo' is not found".getBytes(StandardCharsets.UTF_8))
                .build();
        assertEquals(expected, output);
    }

    @Test
    void GIVEN_connection_created_WHEN_shadow_is_empty_THEN_upsert_document_partial_output() throws JsonProcessingException {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicInteger getCalls = new AtomicInteger();
        when(dataPlaneClient.getThingShadow(eq(getRequest))).then(answer -> {
            GetThingShadowResponse.Builder builder = GetThingShadowResponse.builder();
            if (getCalls.getAndIncrement() == 0) {
                throw ResourceNotFoundException.builder().build();
            } else if (getCalls.getAndIncrement() == 1) {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 1;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, currentId.get());
                }};
                response.state.desired = new HashMap<>(response.state.reported);
                builder.payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)));
            } else {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 2;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, currentId.get());
                    put("output", new HashMap<>() {{
                        put(ConnectionIoT.FIELD_EXIT_CODE, 0);
                    }});
                }};
                response.state.desired = new HashMap<>(response.state.reported);
                builder.payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)));
            }
            return builder.build();
        });
        UpdateShadowResponse shadowResponse = new UpdateShadowResponse();
        shadowResponse.version = 1;
        UpdateThingShadowResponse updateResponse = UpdateThingShadowResponse.builder()
                .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(shadowResponse)))
                .build();
        when(dataPlaneClient.updateThingShadow(any(UpdateThingShadowRequest.class))).then(answer -> {
            UpdateThingShadowRequest request = answer.getArgument(0);
            UpdateShadowRequest payload = connection.mapper().readValue(
                    request.payload().asByteArray(), UpdateShadowRequest.class);
            assertTrue(payload.state.desired.containsKey(ConnectionIoT.FIELD_ID));
            currentId.set(payload.state.desired.get(ConnectionIoT.FIELD_ID).toString());
            return updateResponse;
        });
        CommandOutput output = connection.execute(input);
        CommandOutput expected = CommandOutput.builder()
                .exitCode(0)
                .originalInput(input)
                .build();
        assertEquals(expected, output);
    }

    @Test
    void GIVEN_connection_created_WHEN_get_shadow_fails_THEN_error_is_thrown() {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        doThrow(IotDataPlaneException.class).when(dataPlaneClient).getThingShadow(eq(getRequest));
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_created_WHEN_update_shadow_fails_THEN_error_is_thrown() {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        doThrow(ResourceNotFoundException.class).when(dataPlaneClient).getThingShadow(eq(getRequest));
        doThrow(IotDataPlaneException.class).when(dataPlaneClient).updateThingShadow(any(UpdateThingShadowRequest.class));
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_created_WHEN_shadow_is_never_updated_THEN_connection_times_out()
            throws JsonProcessingException {
        CommandInput input = this.input.withTimeout(Duration.ofSeconds(1));
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicInteger getCalls = new AtomicInteger();
        when(dataPlaneClient.getThingShadow(eq(getRequest))).then(answer -> {
            GetThingShadowResponse.Builder builder = GetThingShadowResponse.builder();
            if (getCalls.getAndIncrement() == 0) {
                throw ResourceNotFoundException.builder().build();
            } else {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 1;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, currentId.get());
                }};
                builder.payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)));
            }
            return builder.build();
        });
        UpdateShadowResponse shadowResponse = new UpdateShadowResponse();
        shadowResponse.version = 1;
        UpdateThingShadowResponse updateResponse = UpdateThingShadowResponse.builder()
                .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(shadowResponse)))
                .build();
        when(dataPlaneClient.updateThingShadow(any(UpdateThingShadowRequest.class))).then(answer -> {
            UpdateThingShadowRequest request = answer.getArgument(0);
            UpdateShadowRequest payload = connection.mapper().readValue(
                    request.payload().asByteArray(), UpdateShadowRequest.class);
            assertTrue(payload.state.desired.containsKey(ConnectionIoT.FIELD_ID));
            currentId.set(payload.state.desired.get(ConnectionIoT.FIELD_ID).toString());
            return updateResponse;
        });
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_created_WHEN_shadow_version_is_invalid_THEN_connection_throws()
            throws JsonProcessingException {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicInteger getCalls = new AtomicInteger();
        when(dataPlaneClient.getThingShadow(eq(getRequest))).then(answer -> {
            GetThingShadowResponse.Builder builder = GetThingShadowResponse.builder();
            if (getCalls.getAndIncrement() == 0) {
                throw ResourceNotFoundException.builder().build();
            } else {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 3;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, currentId.get());
                }};
                builder.payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)));
            }
            return builder.build();
        });
        UpdateShadowResponse shadowResponse = new UpdateShadowResponse();
        shadowResponse.version = 1;
        UpdateThingShadowResponse updateResponse = UpdateThingShadowResponse.builder()
                .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(shadowResponse)))
                .build();
        when(dataPlaneClient.updateThingShadow(any(UpdateThingShadowRequest.class))).then(answer -> {
            UpdateThingShadowRequest request = answer.getArgument(0);
            UpdateShadowRequest payload = connection.mapper().readValue(
                    request.payload().asByteArray(), UpdateShadowRequest.class);
            assertTrue(payload.state.desired.containsKey(ConnectionIoT.FIELD_ID));
            currentId.set(payload.state.desired.get(ConnectionIoT.FIELD_ID).toString());
            return updateResponse;
        });
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_created_WHEN_shadow_is_deleted_THEN_connection_throws()
            throws JsonProcessingException {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        when(dataPlaneClient.getThingShadow(eq(getRequest))).then(answer -> {
            throw ResourceNotFoundException.builder().build();
        });
        UpdateShadowResponse shadowResponse = new UpdateShadowResponse();
        shadowResponse.version = 1;
        UpdateThingShadowResponse updateResponse = UpdateThingShadowResponse.builder()
                .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(shadowResponse)))
                .build();
        when(dataPlaneClient.updateThingShadow(any(UpdateThingShadowRequest.class))).then(answer -> {
            UpdateThingShadowRequest request = answer.getArgument(0);
            UpdateShadowRequest payload = connection.mapper().readValue(
                    request.payload().asByteArray(), UpdateShadowRequest.class);
            assertTrue(payload.state.desired.containsKey(ConnectionIoT.FIELD_ID));
            return updateResponse;
        });
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_created_WHEN_id_changes_THEN_connection_throws()
            throws JsonProcessingException {
        GetThingShadowRequest getRequest = GetThingShadowRequest.builder()
                .shadowName(connection.shadowName())
                .thingName(connection.host().deviceId())
                .build();
        AtomicReference<String> currentId = new AtomicReference<>();
        AtomicInteger getCalls = new AtomicInteger();
        when(dataPlaneClient.getThingShadow(eq(getRequest))).then(answer -> {
            if (getCalls.getAndIncrement() == 0) {
                throw ResourceNotFoundException.builder().build();
            } else {
                GetShadowResponse response = new GetShadowResponse();
                response.version = 2;
                response.state = new ShadowStateWithDelta();
                response.state.reported = new HashMap<>() {{
                    put(ConnectionIoT.FIELD_ID, UUID.randomUUID().toString());
                }};
                return GetThingShadowResponse.builder()
                        .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(response)))
                        .build();
            }
        });
        UpdateShadowResponse shadowResponse = new UpdateShadowResponse();
        shadowResponse.version = 1;
        UpdateThingShadowResponse updateResponse = UpdateThingShadowResponse.builder()
                .payload(SdkBytes.fromByteArray(connection.mapper().writeValueAsBytes(shadowResponse)))
                .build();
        when(dataPlaneClient.updateThingShadow(any(UpdateThingShadowRequest.class))).then(answer -> {
            UpdateThingShadowRequest request = answer.getArgument(0);
            UpdateShadowRequest payload = connection.mapper().readValue(
                    request.payload().asByteArray(), UpdateShadowRequest.class);
            assertTrue(payload.state.desired.containsKey(ConnectionIoT.FIELD_ID));
            return updateResponse;
        });
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }
}
