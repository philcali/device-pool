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
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@ApiModel
@Value.Immutable
abstract class ConnectionShadowModel implements Connection {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionShadow.class);
    abstract IotDataPlaneClient dataPlaneClient();

    abstract Host host();

    @Nullable
    abstract String shadowName();

    abstract ObjectMapper mapper();

    @Override
    public CommandOutput execute(CommandInput input) throws ConnectionException {
        UUID currentId = UUID.randomUUID();
        GetThingShadowRequest getShadow = GetThingShadowRequest.builder()
                .shadowName(shadowName())
                .thingName(host().deviceId())
                .build();
        int version = 1;
        try {
            GetThingShadowResponse currentShadow = dataPlaneClient().getThingShadow(getShadow);
            GetShadowResponse parsedPayload = mapper().readValue(
                    currentShadow.payload().asInputStream(),
                    GetShadowResponse.class);
            version = Optional.ofNullable(parsedPayload.version).orElse(version);
        } catch (ResourceNotFoundException nfe) {
            LOGGER.warn("There is no execution state for {} - {}", host().deviceId(), shadowName());
        } catch (IOException ie) {
            throw new ConnectionException(ie);
        }

        ShadowState state = new ShadowState();
        state.desired = new HashMap<>() {{
            put(ConnectionIoT.FIELD_ID, currentId.toString());
            put(ConnectionIoT.FIELD_LINE, input.line());
        }};
        Optional.ofNullable(input.args())
                .ifPresent(args -> state.desired.put(ConnectionIoT.FIELD_ARGS, args));
        Optional.ofNullable(input.input()).ifPresent(bytes -> {
            String encodedBytes = Base64.getEncoder().encodeToString(bytes);
            state.desired.put(ConnectionIoT.FIELD_INPUT, encodedBytes);
        });
        UpdateShadowRequest updatePayload = new UpdateShadowRequest();
        updatePayload.version = version;
        updatePayload.state = state;
        try {
            UpdateThingShadowRequest updateShadow = UpdateThingShadowRequest.builder()
                    .shadowName(shadowName())
                    .thingName(host().deviceId())
                    .payload(SdkBytes.fromByteArray(mapper().writeValueAsBytes(updatePayload)))
                    .build();
            UpdateThingShadowResponse updatedShadowResponse = dataPlaneClient().updateThingShadow(updateShadow);
        } catch (IOException ie) {

        }
        return null;
    }
}
