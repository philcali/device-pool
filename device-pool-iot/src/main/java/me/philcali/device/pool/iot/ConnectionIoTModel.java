/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@ApiModel
@Value.Immutable
abstract class ConnectionIoTModel implements Connection, Consumer<MqttMessage> {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionIoT.class);
    static final String FIELD_ID = "id";
    static final String FIELD_LINE = "line";
    static final String FIELD_ARGS = "args";
    static final String FIELD_INPUT = "input";
    static final String FIELD_EXIT_CODE = "exitCode";
    static final String FIELD_STDOUT = "stdout";
    static final String FIELD_STDERR = "stderr";
    private final Map<String, CompletableFuture<JsonNode>> executions = new ConcurrentHashMap<>();

    abstract MqttClientConnection connection();

    abstract QualityOfService qualityOfService();

    abstract String topic();

    abstract ObjectMapper mapper();

    @Override
    public CommandOutput execute(CommandInput input) throws ConnectionException {
        UUID commandId = UUID.randomUUID();
        ObjectNode node = mapper().createObjectNode();
        node.put(FIELD_ID, commandId.toString());
        node.put(FIELD_LINE, input.line());
        Optional.ofNullable(input.args()).ifPresent(args -> {
            final ArrayNode array = node.putArray(FIELD_ARGS);
            args.forEach(array::add);
        });
        Optional.ofNullable(input.input()).ifPresent(bytes -> node.put(FIELD_INPUT, bytes));
        try {
            byte[] payload = mapper().writeValueAsBytes(node);
            CompletableFuture<Integer> pr = connection().publish(new MqttMessage(topic(), payload, qualityOfService()));
            pr.get(input.timeout().toSeconds(), TimeUnit.SECONDS);
            // TODO: Need a better way to do this than to block the thread... will have to wait for v2
            final CompletableFuture<JsonNode> result = new CompletableFuture<>();
            executions.putIfAbsent(commandId.toString(), result);
            JsonNode response = result.get(input.timeout().toSeconds(), TimeUnit.SECONDS);
            final CommandOutput.Builder builder = CommandOutput.builder()
                    .exitCode(response.get(FIELD_EXIT_CODE).asInt())
                    .originalInput(input);
            if (response.has(FIELD_STDOUT)) {
                builder.stdout(response.get(FIELD_STDOUT).binaryValue());
            }
            if (response.has(FIELD_STDERR)) {
                builder.stderr(response.get(FIELD_STDERR).binaryValue());
            }
            return builder.build();
        } catch (InterruptedException | TimeoutException e) {
            throw new ConnectionException("Command timed out");
        } catch (IOException e) {
            throw new ConnectionException(e);
        } catch (ExecutionException e) {
            throw new ConnectionException(e.getCause());
        } finally {
            // Pop any extraneous executions
            executions.remove(commandId.toString());
        }
    }

    @Override
    public void accept(MqttMessage mqttMessage) {
        try {
            JsonNode node = mapper().readTree(mqttMessage.getPayload());
            final String requestId = node.get(FIELD_ID).asText();
            final CompletableFuture<JsonNode> responsePresent = executions.remove(requestId);
            if (responsePresent != null) {
                responsePresent.complete(node);
            }
            LOGGER.info("Command result received {}", requestId);
        } catch (IOException e) {
            LOGGER.error("Failed to parse return", e);
        }
    }

    @Override
    public void close() throws Exception {
        CompletableFuture.allOf(
                connection().unsubscribe(topic()),
                connection().unsubscribe(topic() + ConnectionFactoryIoT.RESULT)
        ).get();
    }
}
