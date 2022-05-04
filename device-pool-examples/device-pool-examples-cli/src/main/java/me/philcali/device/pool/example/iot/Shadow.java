/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.iot;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.example.Iot;
import me.philcali.device.pool.iot.ConnectionFactoryShadow;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.Host;
import picocli.CommandLine;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

import java.net.URI;

@CommandLine.Command(
        name = "shadow",
        description = "Execute commands using the Thing shadow document",
        subcommands = {CommandLine.HelpCommand.class}
)
public class Shadow implements Runnable {
    @CommandLine.ParentCommand
    Iot iot;

    @CommandLine.Option(
            names = "--shadow-name",
            description = "The shadow name housing the execution for commands"
    )
    String shadowName;

    @Override
    public void run() {
        try (ConnectionFactory factory = ConnectionFactoryShadow.builder()
                .shadowName(shadowName)
                .dataPlaneClient(IotDataPlaneClient.builder()
                        .endpointOverride(URI.create("https://" + iot.describeEndpoint()))
                        .build())
                .build()) {
            Host host = iot.createHost();
            Connection connection = factory.connect(host);
            CommandOutput output = connection.execute(iot.createInput());
            System.out.println("Executed on " + host.deviceId() + ": " + output.toUTF8String());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
