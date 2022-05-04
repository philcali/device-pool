/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import org.immutables.value.Value;
import software.amazon.awssdk.iot.Timestamp;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

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
                    .endpointOverride(URI.create("https://" + describeDataEndpoint(client)))
                    .build();
        }
    }

    public static final class Builder
            extends ImmutableConnectionFactoryShadow.Builder
            implements ConfigBuilder<ConnectionFactoryShadow> {
        @Override
        public ConnectionFactoryShadow fromConfig(DevicePoolConfig config) {
            return config.namespace("connection.shadow")
                    .map(entry -> dataPlaneClient(entry.get("endpoint")
                            .map(endpoint -> IotDataPlaneClient.builder()
                                    .endpointOverride(URI.create(endpoint))
                                    .region(entry.get("region")
                                            .map(Region::of)
                                            .orElseGet(DefaultAwsRegionProviderChain.builder().build()::getRegion))
                                    .build())
                            .orElseGet(IotDataPlaneClient::create))
                            .build())
                    .orElseGet(this::build);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    abstract String shadowName();

    @Value.Default
    ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simple = new SimpleModule();
        simple.addDeserializer(Timestamp.class, new JsonDeserializer<>() {
            @Override
            public Timestamp deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
                return new Timestamp(new Date(jsonParser.getLongValue() * 1000));
            }
        });
        simple.addSerializer(Timestamp.class, new JsonSerializer<>() {
            @Override
            public void serialize(Timestamp timestamp, JsonGenerator generator, SerializerProvider provider)
                    throws IOException {
                generator.writeNumber(timestamp.getTime() / 1000);
            }
        });
        mapper.registerModule(simple);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
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
