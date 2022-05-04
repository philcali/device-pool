/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example;

import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.example.local.Copy;
import me.philcali.device.pool.example.local.Execute;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.provision.LocalProvisionService;
import me.philcali.device.pool.ssh.ConnectionFactorySSH;
import org.apache.sshd.client.SshClient;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Local class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "local",
        description = "An example app that uses a local static pool to provision",
        subcommands = { Execute.class, Copy.class, CommandLine.HelpCommand.class }
)
public class Local {
    @CommandLine.Option(
            names = {"-u", "--user"},
            description = "SSH username for the selected hosts")
    String userName;

    @CommandLine.Option(
            names = {"-P", "--port"},
            description = "SSH port to use",
            defaultValue = "22"
    )
    int port;

    @CommandLine.Option(
            names = {"-p", "--password"},
            description = "SSH password for the selected hosts",
            interactive = true)
    char[] password;

    @CommandLine.Option(
            names = {"-n", "--host"},
            required = true,
            description = "IP addresses of the hosts representing this pool")
    String[] hostNames;

    /**
     * <p>hostNames.</p>
     *
     * @return a {@link java.util.List} object
     */
    public List<String> hostNames() {
        return Arrays.asList(hostNames);
    }

    /**
     * <p>createPool.</p>
     *
     * @return a {@link me.philcali.device.pool.DevicePool} object
     */
    public DevicePool createPool() {
        SshClient sshClient = SshClient.setUpDefaultClient();
        if (Objects.nonNull(password)) {
            sshClient.addPasswordIdentity(new String(password));
        }
        if (Objects.isNull(userName)) {
            userName = System.getProperty("user.name");
        }
        List<Host> hosts = new ArrayList<>();
        for (int i = 0; i < hostNames.length; i++) {
            hosts.add(Host.builder()
                    .deviceId("host-" + i)
                    .hostName(hostNames[i])
                    .port(port)
                    .platform(PlatformOS.of("unknown", "unknown"))
                    .build());
        }
        // Local provision service handles static pools
        // SSH connection factory for device communication
        return BaseDevicePool.builder()
                .provisionAndReservationService(LocalProvisionService.builder()
                        .addAllHosts(hosts)
                        .build())
                .connectionAndContentFactory(ConnectionFactorySSH.builder()
                        .client(sshClient)
                        .userName(userName)
                        .build())
                .build();
    }
}
