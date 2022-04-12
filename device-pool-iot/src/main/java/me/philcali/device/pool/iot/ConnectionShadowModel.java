/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;

import javax.annotation.Nullable;
import java.util.UUID;

@ApiModel
@Value.Immutable
abstract class ConnectionShadowModel implements Connection {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionShadow.class);
    abstract IotDataPlaneClient dataPlaneClient();

    abstract Host host();

    @Nullable
    abstract String shadowName();

    @Override
    public CommandOutput execute(CommandInput input) throws ConnectionException {
        UUID currentId = UUID.randomUUID();
        GetThingShadowRequest getShadow = GetThingShadowRequest.builder()
                .shadowName(shadowName())
                .thingName(host().deviceId())
                .build();
        try {
            GetThingShadowResponse currentShadow = dataPlaneClient().getThingShadow(getShadow);
        } catch (ResourceNotFoundException nfe) {
            LOGGER.warn("There is no execution state for {} - {}", host().deviceId(), shadowName());
        }
        return null;
    }
}
