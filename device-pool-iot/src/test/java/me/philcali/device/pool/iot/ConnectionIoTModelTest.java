/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ConnectionIoTModelTest {
    private ConnectionIoT connection;
    @Mock
    private MqttClientConnection mqttClientConnection;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        connection = ConnectionIoT.builder()
                .connection(mqttClientConnection)
                .qualityOfService(QualityOfService.AT_LEAST_ONCE)
                .topic("command/deviceId/execute")
                .mapper(mapper)
                .build();
    }

    @Test
    void GIVEN_connection_is_created_WHEN_execute_THEN_round_trip()
            throws ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello", "World")
                .input(new byte[] { 'a', 'b', 'c' })
                .build();
        AtomicReference<String> commandId = new AtomicReference<>();
        when(mqttClientConnection.publish(any(MqttMessage.class))).then(answer -> {
            MqttMessage message = answer.getArgument(0);
            assertEquals("command/deviceId/execute", message.getTopic());
            assertEquals(QualityOfService.AT_LEAST_ONCE, message.getQos());
            JsonNode node = mapper.readTree(message.getPayload());
            assertTrue(node.has("id"), "Payload does not contain an id: " + node);
            commandId.set(node.get("id").asText());
            CompletableFuture<Integer> result = new CompletableFuture<>();
            result.complete(0);
            return result;
        });
        CompletableFuture<CommandOutput> output = CompletableFuture
                .supplyAsync(() -> connection.execute(input));
        CommandOutput expectedOutput = CommandOutput.builder()
                .exitCode(0)
                .stdout("Hello World".getBytes(StandardCharsets.UTF_8))
                .stderr("command 'echo' not found".getBytes(StandardCharsets.UTF_8))
                .originalInput(input)
                .build();
        ObjectNode node = mapper.createObjectNode();
        node.set("id", new TextNode("non-exist"));
        byte[] payload = mapper.writeValueAsBytes(node);
        connection.accept(new MqttMessage("command/deviceId/execute", payload, QualityOfService.AT_LEAST_ONCE));
        new Thread(() -> {
            String nodeId;
            do {
                nodeId = commandId.get();
            } while (Objects.isNull(nodeId));
            final ObjectNode realNode = mapper.createObjectNode();
            realNode.put("id", nodeId);
            realNode.put("exitCode", 0);
            realNode.put("stdout", "Hello World".getBytes(StandardCharsets.UTF_8));
            realNode.put("stderr", "command 'echo' not found".getBytes(StandardCharsets.UTF_8));
            try {
                byte[] realPayload = mapper.writeValueAsBytes(realNode);
                connection.accept(new MqttMessage("command/deviceId/execute", realPayload, QualityOfService.AT_LEAST_ONCE));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }).start();
        assertEquals(expectedOutput, output.get(10, TimeUnit.SECONDS));
    }

    @Test
    void GIVEN_connection_is_created_WHEN_result_never_returns_THEN_command_times_out() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello", "World")
                .input(new byte[] { 'a', 'b', 'c' })
                .timeout(Duration.ofSeconds(1))
                .build();
        when(mqttClientConnection.publish(any(MqttMessage.class))).then(answer -> {
            MqttMessage message = answer.getArgument(0);
            assertEquals("command/deviceId/execute", message.getTopic());
            assertEquals(QualityOfService.AT_LEAST_ONCE, message.getQos());
            JsonNode node = mapper.readTree(message.getPayload());
            assertTrue(node.has("id"), "Payload does not contain an id: " + node);
            CompletableFuture<Integer> result = new CompletableFuture<>();
            result.complete(0);
            return result;
        });
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_is_created_WHEN_result_is_garbage_THEN_exception_is_thrown() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello", "World")
                .input(new byte[] { 'a', 'b', 'c' })
                .build();
        AtomicReference<String> commandId = new AtomicReference<>();
        when(mqttClientConnection.publish(any(MqttMessage.class))).then(answer -> {
            MqttMessage message = answer.getArgument(0);
            assertEquals("command/deviceId/execute", message.getTopic());
            assertEquals(QualityOfService.AT_LEAST_ONCE, message.getQos());
            JsonNode node = mapper.readTree(message.getPayload());
            assertTrue(node.has("id"), "Payload does not contain an id: " + node);
            commandId.set(node.get("id").asText());
            CompletableFuture<Integer> result = new CompletableFuture<>();
            result.completeExceptionally(new RuntimeException("fail"));
            return result;
        });
        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_is_created_WHEN_accept_is_garbage_THEN_error_is_logged() {
        byte[] payload = new byte[] { '\n', '^' };
        connection.accept(new MqttMessage("farts", payload, QualityOfService.AT_LEAST_ONCE));
    }

    @Test
    void GIVEN_connection_is_created_WHEN_close_THEN_services_are_closed() throws Exception {
        CompletableFuture<Integer> unsubFirst = new CompletableFuture<>();
        CompletableFuture<Integer> unsubSecond = new CompletableFuture<>();
        unsubFirst.complete(0);
        unsubSecond.complete(0);
        doReturn(unsubFirst).when(mqttClientConnection).unsubscribe(eq("command/deviceId/execute"));
        doReturn(unsubSecond).when(mqttClientConnection).unsubscribe(eq("command/deviceId/execute/result"));
        connection.close();
    }
}
