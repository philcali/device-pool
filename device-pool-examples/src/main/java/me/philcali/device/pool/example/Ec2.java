/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example;

import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.ec2.AutoscalingProvisionService;
import me.philcali.device.pool.ec2.Ec2ReservationService;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.ssh.ConnectionFactorySSH;
import picocli.CommandLine;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(
        name = "ec2",
        description = "Demonstrates autoscaling and ec2 device pools"
)
public class Ec2 implements Runnable {
    @CommandLine.Option(
            names = {"-g", "--group"},
            description = "AutoScaling group name to back provisioning",
            required = true)
    String groupName;

    @CommandLine.Option(
            names = {"-p", "--platform"},
            description = "Platform / OS combo in the form of 'os:arch', eg: 'unix:armv7'",
            converter = PlatformOSConverter.class,
            required = true)
    PlatformOS platformOS;

    @CommandLine.Option(names = {"-i", "--identity"})
    KeyPair pemFile;

    static class PlatformOSConverter implements CommandLine.ITypeConverter<PlatformOS> {
        @Override
        public PlatformOS convert(String str) {
            String[] parts = str.split(":");
            if (parts.length < 2) {
                throw new CommandLine.TypeConversionException(
                        "platform / os should be in the form of 'os:arch', but was " + str);
            }
            return PlatformOS.of(parts[0], parts[1]);
        }
    }

    @Override
    public void run() {
        List<KeyPair> publicKeys = new ArrayList<>();
        if (Objects.nonNull(pemFile)) {
            publicKeys.add(pemFile);
        }
        DevicePool pool = BaseDevicePool.builder()
                .provisionService(AutoscalingProvisionService.of(groupName))
                .reservationService(Ec2ReservationService.of(platformOS))
                .connectionAndContentFactory(ConnectionFactorySSH.builder()
                        .addAllPublicKeys(publicKeys)
                        .build())
                .build();
        List<Device> devices = pool.provisionWait(ProvisionInput.builder()
                .id("test-ec2")
                .build(), 30, TimeUnit.SECONDS);
        for (Device device : devices) {
            System.out.println("Successfully provisioned " + device.id());
        }
    }
}
