/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.lab;

import me.philcali.device.pool.example.util.PlatformOSConverter;
import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.client.DeviceLabProvisionService;
import me.philcali.device.pool.example.Lab;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.ssh.ConnectionFactorySSH;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Provision class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "provision",
        description = "Creates a provision on a pool and demonstrates acquisition",
        subcommands = {CommandLine.HelpCommand.class}
)
public class Provision implements Runnable {
    @CommandLine.ParentCommand
    Lab deviceLab;

    @CommandLine.Option(
            names = {"-p", "--platform"},
            description = "Platform / OS combo in the form of 'os:arch', eg: 'unix:armv7'",
            converter = PlatformOSConverter.class,
            required = true)
    PlatformOS platformOS;

    @CommandLine.Option(
            names = "--amount",
            description = "Number of devices to provision",
            defaultValue = "1")
    int amount;

    /** {@inheritDoc} */
    @Override
    public void run() {
        DevicePool pool = BaseDevicePool.builder()
                .provisionAndReservationService(DeviceLabProvisionService.builder()
                        .poolId(deviceLab.poolName())
                        .deviceLabService(deviceLab.createService())
                        .platform(platformOS)
                        .build())
                .connectionAndContentFactory(ConnectionFactorySSH.create())
                .build();
        List<Device> devices = pool.provisionSync(ProvisionInput.builder()
                .amount(amount)
                .build(), 5, TimeUnit.MINUTES);
        for (Device device : devices) {
            System.out.println("Provisioned device through control plane " + device.id());
        }
    }
}
