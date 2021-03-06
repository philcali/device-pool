/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example;

import me.philcali.device.pool.example.lab.Destroy;
import me.philcali.device.pool.example.lab.Init;
import me.philcali.device.pool.example.lab.Provision;
import me.philcali.device.pool.service.client.AwsV4SigningInterceptor;
import me.philcali.device.pool.service.client.DeviceLabService;
import picocli.CommandLine;

/**
 * <p>Lab class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "lab",
        description = "Interact with the device-lab control plane",
        subcommands = {Init.class, Provision.class, Destroy.class, CommandLine.HelpCommand.class}
)
public class Lab {
    @CommandLine.Option(
            names = "--endpoint",
            required = true,
            description = "Override for device lab endpoint",
            scope = CommandLine.ScopeType.INHERIT)
    String endpoint;

    @CommandLine.Option(
            names = {"-d", "--name"},
            description = "Name of the device pool",
            defaultValue = "examplePool",
            scope = CommandLine.ScopeType.INHERIT)
    String poolName;

    /**
     * <p>poolName.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String poolName() {
        return poolName;
    }

    /**
     * <p>createService.</p>
     *
     * @return a {@link me.philcali.device.pool.service.client.DeviceLabService} object
     */
    public DeviceLabService createService() {
        return DeviceLabService.create((client, builder) -> {
            client.addInterceptor(AwsV4SigningInterceptor.create());
            builder.baseUrl(endpoint);
        });
    }
}
