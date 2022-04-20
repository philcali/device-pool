/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.iot.iotshadow.model.GetShadowResponse;
import software.amazon.awssdk.iot.iotshadow.model.ShadowState;
import software.amazon.awssdk.iot.iotshadow.model.UpdateShadowRequest;
import software.amazon.awssdk.iot.iotshadow.model.UpdateShadowResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.IotDataPlaneException;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ApiModel
@Value.Immutable
abstract class ConnectionShadowModel implements Connection {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionShadow.class);
    abstract IotDataPlaneClient dataPlaneClient();

    abstract Host host();

    @Nullable
    abstract String shadowName();

    abstract ObjectMapper mapper();

    private Optional<GetShadowResponse> getThingShadow() {
        GetThingShadowRequest getShadow = GetThingShadowRequest.builder()
                .shadowName(shadowName())
                .thingName(host().deviceId())
                .build();
        GetShadowResponse parsedPayload = null;
        try {
            GetThingShadowResponse currentShadow = dataPlaneClient().getThingShadow(getShadow);
            parsedPayload = mapper().readValue(
                    currentShadow.payload().asInputStream(),
                    GetShadowResponse.class);
        } catch (ResourceNotFoundException nfe) {
            LOGGER.warn("There is no execution state for {} - {}", host().deviceId(), shadowName());
        } catch (IotDataPlaneException | IOException ie) {
            throw new ConnectionException(ie);
        }
        return Optional.ofNullable(parsedPayload);
    }

    private UpdateShadowRequest convertInputToUpdate(UUID currentId, int version, CommandInput input) {
        Map<String,Object> commandInput = new HashMap<>() {{
            put(ConnectionIoT.FIELD_LINE, input.line());
        }};
        Optional.ofNullable(input.args())
                .ifPresent(args -> commandInput.put(ConnectionIoT.FIELD_ARGS, args));
        Optional.ofNullable(input.input()).ifPresent(bytes -> {
            String encodedBytes = Base64.getEncoder().encodeToString(bytes);
            commandInput.put(ConnectionIoT.FIELD_INPUT, encodedBytes);
        });
        UpdateShadowRequest updatePayload = new UpdateShadowRequest();
        updatePayload.version = version;
        updatePayload.state = new ShadowState();
        updatePayload.thingName = host().deviceId();
        updatePayload.state.desired = new HashMap<>() {{
            put(ConnectionIoT.FIELD_ID, currentId.toString());
            put("input", commandInput);
        }};
        return updatePayload;
    }

    public UpdateShadowResponse updateThingShadow(UpdateShadowRequest request) {
        try {
            UpdateThingShadowRequest updateShadow = UpdateThingShadowRequest.builder()
                    .shadowName(shadowName())
                    .thingName(host().deviceId())
                    .payload(SdkBytes.fromByteArray(mapper().writeValueAsBytes(request)))
                    .build();
            UpdateThingShadowResponse updatedShadowResponse = dataPlaneClient().updateThingShadow(updateShadow);
            return mapper().readValue(updatedShadowResponse.payload().asInputStream(), UpdateShadowResponse.class);
        } catch (IotDataPlaneException | IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public CommandOutput execute(CommandInput input) throws ConnectionException {
        UUID currentId = UUID.randomUUID();
        int version = getThingShadow().map(payload -> payload.version).orElse(1);
        UpdateShadowResponse response = updateThingShadow(convertInputToUpdate(currentId, version, input));
        CompletableFuture<CommandOutput> futureResult = CompletableFuture.supplyAsync(() -> {
            for (;;) {
                GetShadowResponse currentShadow = getThingShadow()
                        .filter(doc -> currentId.toString().equals(doc.state.reported.get(ConnectionIoT.FIELD_ID)))
                        .filter(doc -> Objects.equals(doc.version, response.version)
                                || Objects.equals(doc.version, response.version + 1))
                        .orElseThrow(() -> new ConnectionException(1, "Command is no longer valid", input));
                LOGGER.debug("Found shadow document {} for {}/{} at {}",
                        currentId, host().deviceId(), shadowName(), currentShadow.version);
                if (currentShadow.state.reported.containsKey("output")) {
                    Map<String, Object> output = (Map<String, Object>) currentShadow.state.reported.get("output");
                    LOGGER.debug("Found shadow output document {} for {}/{}: {}",
                            currentId, host().deviceId(), shadowName(), output);
                    CommandOutput.Builder builder = CommandOutput.builder()
                            .exitCode((int) output.get("exitCode"))
                            .originalInput(input);
                    if (output.containsKey(ConnectionIoT.FIELD_STDOUT)) {
                        builder.stdout(Base64.getDecoder().decode((String) output.get(ConnectionIoT.FIELD_STDOUT)));
                    }
                    if (output.containsKey(ConnectionIoT.FIELD_STDERR)) {
                        builder.stderr(Base64.getDecoder().decode((String) output.get(ConnectionIoT.FIELD_STDERR)));
                    }
                    return builder.build();
                }
            }
        });
        try {
            return futureResult.get(input.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new ConnectionException(1, "Command timed out", input);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConnectionException) {
                throw (ConnectionException) e.getCause();
            }
            throw new ConnectionException(e.getCause());
        }
    }
}
