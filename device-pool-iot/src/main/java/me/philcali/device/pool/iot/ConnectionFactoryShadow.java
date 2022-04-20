/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import org.immutables.value.Value;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

import javax.annotation.Nullable;
import java.net.URI;

@APIShadowModel
@Value.Immutable
public abstract class ConnectionFactoryShadow implements ConnectionFactory {
    private static final String DEFAULT_ENDPOINT_TYPE = "iot:Data-ATS";

    public static String describeDataEndpoint(IotClient client) {
        return client.describeEndpoint(DescribeEndpointRequest.builder()
                .endpointType(DEFAULT_ENDPOINT_TYPE)
                .build())
                .endpointAddress();
    }

    @Value.Default
    IotDataPlaneClient dataPlaneClient() {
        try (IotClient client = IotClient.create()) {
            return IotDataPlaneClient.builder()
                    .endpointOverride(URI.create(describeDataEndpoint(client)))
                    .build();
        }
    }

    public static final class Builder extends ImmutableConnectionFactoryShadow.Builder {

    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    abstract String shadowName();

    @Value.Default
    ObjectMapper mapper() {
        return new ObjectMapper();
    }

    @Override
    public Connection connect(final Host host) throws ConnectionException {
        return ConnectionShadow.builder()
                .dataPlaneClient(dataPlaneClient())
                .shadowName(shadowName())
                .host(host)
                .mapper(mapper())
                .build();
    }

    @Override
    public void close() {
        dataPlaneClient().close();
    }
}
