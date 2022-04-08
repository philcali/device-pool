/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@APIShadowModel
@Value.Immutable
public abstract class ConnectionFactoryIoT implements ConnectionFactory {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionFactoryIoT.class);
    static final String TOPIC_PATTERN = "command/%s/execute";
    static final String RESULT = "/result";

    abstract MqttClientConnection connection();

    @Value.Default
    QualityOfService qualityOfService() {
        return QualityOfService.AT_LEAST_ONCE;
    }

    @Value.Default
    ObjectMapper mapper() {
        return new ObjectMapper();
    }

    public static final class Builder
            extends ImmutableConnectionFactoryIoT.Builder
            implements ConfigBuilder<ConnectionFactoryIoT> {
        @Override
        public ConnectionFactoryIoT fromConfig(DevicePoolConfig config) {
            return config.namespace("connection.iot")
                    .map(entry -> {
                        String certPath = entry.get("certPath")
                                .orElseThrow(() -> new ConnectionException("certPath is required"));
                        String privKeyPath = entry.get("privKeyPath")
                                .orElseThrow(() -> new ConnectionException("privKeyPath is required"));
                        AwsIotMqttConnectionBuilder builder =
                                AwsIotMqttConnectionBuilder.newMtlsBuilderFromPath(certPath, privKeyPath);
                        builder.withClientId(entry.get("clientId").orElseGet(UUID.randomUUID()::toString));
                        builder.withCleanSession(true);
                        builder.withPort(entry.get("port").map(Short::parseShort).orElse((short) 8883));
                        builder.withEndpoint(entry.get("endpoint")
                                .orElseThrow(() -> new ConnectionException("endpoint is required")));
                        MqttClientConnection connection = builder.build();
                        try {
                            connection.connect().get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new ConnectionException(e);
                        }
                        return connection(connection).build();
                    })
                    .orElseThrow(() -> new ConnectionException("Could not complete the creation"));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Connection connect(Host host) throws ConnectionException {
        final String topic = String.format(TOPIC_PATTERN, host.deviceId());
        try {
            ConnectionIoT newConnection = ConnectionIoT.builder()
                    .connection(connection())
                    .qualityOfService(qualityOfService())
                    .topic(topic)
                    .mapper(mapper())
                    .build();
            CompletableFuture<Integer> subscribe = connection().subscribe(topic + RESULT,
                    qualityOfService(), newConnection);
            int result = subscribe.get();
            LOGGER.debug("Successfully subscribed to {}: {}", topic, result);
            return newConnection;
        } catch (ExecutionException | InterruptedException ee) {
            LOGGER.error("Failed to subscribe to {}", topic, ee);
            throw new ConnectionException(ee);
        }
    }

    @Override
    public void close() throws Exception {
        connection().disconnect().get();
        connection().close();
    }
}
