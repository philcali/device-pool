/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.Command;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationRequest;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;
import software.amazon.awssdk.services.ssm.model.SendCommandRequest;
import software.amazon.awssdk.services.ssm.model.SendCommandResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({MockitoExtension.class})
class ConnectionSSMTest {

    @Mock
    private SsmClient ssm;

    private Connection connection;
    private Host host;

    @BeforeEach
    void setup() {
        ConnectionFactory factory = ConnectionFactorySSM.builder()
                .ssm(ssm)
                .build();

        host = Host.builder()
                .hostName("127.0.0.1")
                .port(22)
                .deviceId("i-abcefg123")
                .platform(PlatformOS.builder()
                        .os("Linux")
                        .arch("aarch64")
                        .build())
                .build();

        connection = factory.connect(host);
    }

    private String sendCommand(String command) {
        SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                .timeoutSeconds(30)
                .documentName("AWS-RunShellScript")
                .comment("Command for host " + host.deviceId())
                .instanceIds(host.deviceId())
                .parameters(new HashMap<String, Set<String>>() {{
                    put("commands", Collections.singleton(command));
                }})
                .build();
        SendCommandResponse sentCommand = SendCommandResponse.builder()
                .command(Command.builder()
                        .commandId("commandId")
                        .build())
                .build();

        when(ssm.sendCommand(eq(sendCommandRequest))).thenReturn(sentCommand);
        return sentCommand.command().commandId();
    }

    private void sendCommandFails(String command) {
        SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                .timeoutSeconds(30)
                .documentName("AWS-RunShellScript")
                .comment("Command for host " + host.deviceId())
                .instanceIds(host.deviceId())
                .parameters(new HashMap<String, Set<String>>() {{
                    put("commands", Collections.singleton(command));
                }})
                .build();

        when(ssm.sendCommand(eq(sendCommandRequest))).thenThrow(SsmException.class);
    }

    private void getCommandInvocation(String commandId) {
        GetCommandInvocationRequest getCommand = GetCommandInvocationRequest.builder()
                .commandId(commandId)
                .instanceId(host.deviceId())
                .build();

        GetCommandInvocationResponse inProgress = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.IN_PROGRESS)
                .commandId(commandId)
                .build();

        GetCommandInvocationResponse getResponse = GetCommandInvocationResponse.builder()
                .commandId(commandId)
                .responseCode(0)
                .status(CommandInvocationStatus.SUCCESS)
                .standardOutputContent("Hello World")
                .standardErrorContent("")
                .build();

        when(ssm.getCommandInvocation(eq(getCommand)))
                .thenReturn(inProgress, getResponse);
    }

    private void getFailedCommandInvocation(String commandId) {
        GetCommandInvocationRequest getCommand = GetCommandInvocationRequest.builder()
                .commandId(commandId)
                .instanceId(host.deviceId())
                .build();

        GetCommandInvocationResponse inProgress = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.IN_PROGRESS)
                .commandId(commandId)
                .build();

        GetCommandInvocationResponse getResponse = GetCommandInvocationResponse.builder()
                .commandId(commandId)
                .responseCode(-1)
                .status(CommandInvocationStatus.FAILED)
                .standardOutputContent("")
                .standardErrorContent("-bash: echo: command not found")
                .build();

        when(ssm.getCommandInvocation(eq(getCommand)))
                .thenReturn(inProgress, getResponse);
    }

    private void getCommandFailsOnSsm(String commandId) {
        GetCommandInvocationRequest getCommand = GetCommandInvocationRequest.builder()
                .commandId(commandId)
                .instanceId(host.deviceId())
                .build();

        when(ssm.getCommandInvocation(eq(getCommand))).thenThrow(SsmException.class);
    }

    private void getTimeoutCommandInvocation(String commandId) {
        GetCommandInvocationRequest getCommand = GetCommandInvocationRequest.builder()
                .commandId(commandId)
                .instanceId(host.deviceId())
                .build();

        GetCommandInvocationResponse timeout = GetCommandInvocationResponse.builder()
                .status(CommandInvocationStatus.TIMED_OUT)
                .commandId(commandId)
                .build();

        when(ssm.getCommandInvocation(eq(getCommand))).thenReturn(timeout);
    }

    @Test
    void GIVEN_connection_is_established_WHEN_exec_command_THEN_ssm_document_is_sent() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello").addArgs("World")
                .build();

        getCommandInvocation(sendCommand("echo Hello World"));

        CommandOutput output = connection.execute(input);
        CommandOutput expectedOutput = CommandOutput.builder()
                .exitCode(0)
                .originalInput(input)
                .stdout("Hello World".getBytes(StandardCharsets.UTF_8))
                .stderr("".getBytes(StandardCharsets.UTF_8))
                .build();

        assertEquals(expectedOutput, output);
    }

    @Test
    void GIVEN_connection_is_established_WHEN_exec_command_fails_THEN_output_is_failed() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello").addArgs("World")
                .build();

        getFailedCommandInvocation(sendCommand("echo Hello World"));

        CommandOutput output = connection.execute(input);
        CommandOutput expectedOutput = CommandOutput.builder()
                .exitCode(-1)
                .originalInput(input)
                .stdout("".getBytes(StandardCharsets.UTF_8))
                .stderr("-bash: echo: command not found".getBytes(StandardCharsets.UTF_8))
                .build();

        assertEquals(expectedOutput, output);
    }

    @Test
    void GIVEN_connection_is_established_WHEN_exec_times_out_THEN_exception_is_thrown() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello").addArgs("World")
                .build();

        getTimeoutCommandInvocation(sendCommand("echo Hello World"));

        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_is_established_WHEN_exec_fails_to_send_to_ssm_THEN_exception_is_thrown() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello").addArgs("World")
                .build();

        sendCommandFails("echo Hello World");

        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }

    @Test
    void GIVEN_connection_is_established_WHEN_exec_fails_to_get_from_ssm_THEN_exception_is_thrown() {
        CommandInput input = CommandInput.builder()
                .line("echo")
                .addArgs("Hello").addArgs("World")
                .build();

        getCommandFailsOnSsm(sendCommand("echo Hello World"));

        assertThrows(ConnectionException.class, () -> connection.execute(input));
    }
}
