/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example;

import me.philcali.device.pool.example.iot.Mqtt;
import me.philcali.device.pool.example.iot.Shadow;
import me.philcali.device.pool.example.util.PlatformOSConverter;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import picocli.CommandLine;
import software.amazon.awssdk.services.iot.IotClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "iot",
        description = "Interact with devices that are connected to the AWS IoT data plane",
        subcommands = {Mqtt.class, Shadow.class, CommandLine.HelpCommand.class}
)
public class Iot {
    @CommandLine.Option(
            names = "--thing-name",
            description = "The Thing name for the device to control",
            scope = CommandLine.ScopeType.INHERIT,
            required = true
    )
    String thingName;

    @CommandLine.Option(
            names = "--platform",
            defaultValue = "unknown:unknown",
            description = "Platform of the data Thing in the form of, eg: 'os:arch'",
            scope = CommandLine.ScopeType.INHERIT,
            converter = PlatformOSConverter.class
    )
    PlatformOS platformOS;

    @CommandLine.Parameters(
            paramLabel = "ARGS",
            scope = CommandLine.ScopeType.INHERIT
    )
    String[] commands;

    @CommandLine.Option(
            names = "--timeout",
            defaultValue = "30",
            description = "The timeout of the command in seconds",
            scope = CommandLine.ScopeType.INHERIT
    )
    long timeout;

    public IotClient createClient() {
        return IotClient.create();
    }

    public String describeEndpoint() {
        try (IotClient iot = createClient()) {
            return iot.describeEndpoint(builder -> builder.endpointType("iot:data-ats")).endpointAddress();
        }
    }

    public Host createHost() {
        return Host.of(platformOS, thingName, thingName);
    }

    public CommandInput createInput() {
        return CommandInput.builder()
                .timeout(Duration.ofSeconds(timeout))
                .line(commands[0])
                .args(Arrays.stream(commands).skip(1).collect(Collectors.toList()))
                .build();
    }
}
