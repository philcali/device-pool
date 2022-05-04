/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.iot;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.example.Iot;
import me.philcali.device.pool.iot.ConnectionFactoryIoT;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.Host;
import picocli.CommandLine;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.util.UUID;

@CommandLine.Command(
        name = "mqtt",
        description = "Issues command execution over the MQTT data plane connection",
        subcommands = {CommandLine.HelpCommand.class}
)
public class Mqtt implements Runnable {
    @CommandLine.Option(
            names = "--cert-path",
            description = "Provide the control Thing certificate",
            required = true
    )
    String certPath;

    @CommandLine.Option(
            names = "--priv-key-path",
            description = "Provide the control Thing private key path",
            required = true
    )
    String privateKeyPath;

    @CommandLine.Option(
            names = "--port",
            defaultValue = "8883",
            description = "The port for the control MQTT connection"
    )
    short port;

    @CommandLine.ParentCommand
    Iot iot;

    @Override
    public void run() {
        try (AwsIotMqttConnectionBuilder builder = AwsIotMqttConnectionBuilder
                .newMtlsBuilderFromPath(certPath, privateKeyPath);
                MqttClientConnection connection = builder
                        .withEndpoint(iot.describeEndpoint())
                        .withPort(port)
                        .withClientId(UUID.randomUUID().toString())
                        .withCleanSession(true)
                        .build();
                ConnectionFactory factory = ConnectionFactoryIoT.builder()
                        .connection(connection)
                        .build()) {
            Host host = iot.createHost();
            CommandInput input = iot.createInput();
            System.out.println("Executing on " + host.deviceId() + ": '" + input + "'");
            Connection exec = factory.connect(host);
            CommandOutput output =  exec.execute(input);
            System.out.println("Executed on " + host.deviceId() + ": " + output.toUTF8String());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
