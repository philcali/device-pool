package me.philcali.device.pool.ssm;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationRequest;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;
import software.amazon.awssdk.services.ssm.model.SendCommandRequest;
import software.amazon.awssdk.services.ssm.model.SendCommandResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@ApiModel
@Value.Immutable
abstract class ConnectionSSMModel implements Connection {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionSSM.class);

    abstract SsmClient ssm();

    abstract Host host();

    abstract String documentName();

    abstract Waiter<GetCommandInvocationResponse> waiter();

    private WaiterOverrideConfiguration waiterOverride(CommandInput input) {
        return WaiterOverrideConfiguration.builder()
                .waitTimeout(input.timeout())
                .build();
    }

    private Function<GetCommandInvocationResponse, CommandOutput> convert(CommandInput input) {
        return response -> CommandOutput.builder()
                .originalInput(input)
                .exitCode(response.responseCode())
                .stdout(response.standardOutputContent().getBytes(StandardCharsets.UTF_8))
                .stderr(response.standardErrorContent().getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Override
    public CommandOutput execute(CommandInput input) throws ConnectionException {
        try {
            final StringBuilder commands = new StringBuilder(input.line());
            Optional.ofNullable(input.args()).ifPresent(args -> {
                commands.append(" ").append(String.join(" ", args));
            });
            SendCommandResponse sentCommand = ssm().sendCommand(SendCommandRequest.builder()
                    .comment("Command for host " + host().deviceId())
                    .documentName(documentName())
                    .instanceIds(host().deviceId())
                    .timeoutSeconds((int) input.timeout().toSeconds())
                    .parameters(new HashMap<>() {{
                        put("commands", Collections.singleton(commands.toString()));
                    }})
                    .build());
            LOGGER.info("Sent command to {}: {}", host().deviceId(), sentCommand.command().commandId());
            Supplier<GetCommandInvocationResponse> supplier = () -> ssm().getCommandInvocation(
                    GetCommandInvocationRequest.builder()
                            .commandId(sentCommand.command().commandId())
                            .instanceId(host().deviceId())
                            .build());
            WaiterResponse<GetCommandInvocationResponse> response = waiter().run(supplier, waiterOverride(input));
            ResponseOrException<GetCommandInvocationResponse> either = response.matched();
            LOGGER.debug("Waiter terminated after {} attempts", response.attemptsExecuted());
            either.exception().ifPresent(ex -> {
                throw new ConnectionException(ex);
            });
            return either.response()
                    .map(convert(input))
                    .orElseThrow(() -> new ConnectionException("Failed to execute: " + commands));
        } catch (SsmException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public void close() {
        // No-op
    }
}
