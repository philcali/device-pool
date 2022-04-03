/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.cli;

import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.client.DeviceLabProvisionService;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.s3.ContentTransferAgentFactoryS3;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.client.DeviceLabService;
import me.philcali.device.pool.ssh.ConnectionFactorySSH;
import me.philcali.device.pool.ssm.ConnectionFactorySSM;
import org.apache.sshd.client.SshClient;
import picocli.CommandLine;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "devices",
        description = "Device Lab CLI for the data plane",
        subcommands = {CommandLine.HelpCommand.class}
)
public class Devices {
    private static final String REMOTE_PREFIX = "remote:";

    @CommandLine.ParentCommand
    DeviceLabCLI cli;

    @CommandLine.Option(
            names = "--pool-id",
            description = "name of the device pool",
            scope = CommandLine.ScopeType.INHERIT
    )
    String poolId;

    @CommandLine.Option(
            names = "--s3-bucket",
            description = "name of the s3 bucket for file transfer",
            scope = CommandLine.ScopeType.INHERIT
    )
    String bucketName;

    @CommandLine.Option(
            names = {"-passwd", "--password"},
            description = "password of the SSH user",
            interactive = true,
            scope = CommandLine.ScopeType.INHERIT
    )
    String password;

    @CommandLine.Option(
            names = {"-P", "--port"},
            defaultValue = "22",
            description = "port of the SSH client connection",
            scope = CommandLine.ScopeType.INHERIT
    )
    Integer port;

    @CommandLine.Option(
            names = {"-u", "--user"},
            description = "user for the SSH host",
            scope = CommandLine.ScopeType.INHERIT
    )
    String userName;

    @CommandLine.Option(
            names = "--use-ssm",
            description = "communicate connections using SSM",
            scope = CommandLine.ScopeType.INHERIT
    )
    boolean useSSM;

    @CommandLine.Option(
            names = "--amount",
            description = "amount of devices in the pool to reserve",
            defaultValue = "1",
            scope = CommandLine.ScopeType.INHERIT
    )
    int amount;

    @CommandLine.Option(
            names = {"-pt", "--provision-timeout"},
            description = "timeout waiting for the operation in seconds",
            defaultValue = "30",
            scope = CommandLine.ScopeType.INHERIT
    )
    int provisionTimeout;

    @CommandLine.Option(
            names = "--all",
            description = "reserve all devices for this operation",
            scope = CommandLine.ScopeType.INHERIT
    )
    boolean allDevices;

    @CommandLine.Option(
            names = {"-p", "--platform"},
            description = "target platform of the device in form of 'os:arch', eg: 'linux:armv6'",
            scope = CommandLine.ScopeType.INHERIT
    )
    String platform;

    private String configuredPoolId() {
        if (poolId != null) {
            return poolId;
        }
        return cli.loadConfig()
                .flatMap(config -> config.namespace("provision.lab"))
                .flatMap(entry -> entry.get("poolId"))
                .orElseThrow(() -> new IllegalStateException("Could not find a pool to use"));
    }

    protected DevicePool createPool(DeviceLabService service) {
        // First attempt to load the entire pool from properties
        return cli.loadConfig().flatMap(config -> {
            try {
                return Optional.of(DevicePool.create(config));
            } catch (IllegalStateException | ProvisioningException e) {
                return Optional.empty();
            }
        }).orElseGet(() -> {
            // Unable to create a full DevicePool, resort to overriding things found in file
            DeviceLabProvisionService.Builder provisionBuilder = DeviceLabProvisionService.builder()
                    .deviceLabService(service);
            Optional.ofNullable(poolId).ifPresent(provisionBuilder::poolId);
            Optional.ofNullable(platform).map(PlatformOS::fromString).ifPresent(provisionBuilder::platform);
            Optional.ofNullable(port).ifPresent(provisionBuilder::port);
            ConnectionFactory connections;
            ContentTransferAgentFactory transfers = null;
            Supplier<ConnectionFactorySSH> getter = () -> {
                SshClient sshClient = SshClient.setUpDefaultClient();
                if (Objects.nonNull(password)) {
                    sshClient.addPasswordIdentity(password);
                }
                return ConnectionFactorySSH.builder()
                        .client(sshClient)
                        .userName(Optional.ofNullable(userName).orElseGet(() -> System.getProperty("user.name")))
                        .build();
            };
            if (useSSM) {
                connections = ConnectionFactorySSM.create();
            } else {
                final ConnectionFactorySSH ssh = getter.get();
                connections = ssh;
                transfers = ssh;
            }
            if (Objects.nonNull(bucketName)) {
                transfers = ContentTransferAgentFactoryS3.builder()
                        .bucketName(bucketName)
                        .build();
            } else if (Objects.isNull(transfers)) {
                transfers = getter.get();
            }
            return BaseDevicePool.builder()
                    .provisionAndReservationService(provisionBuilder.build())
                    .transfers(transfers)
                    .connections(connections)
                    .build();
        });
    }

    protected void provisionOnFrom(DevicePool pool, DeviceLabService service, Consumer<Device> thunk) {
        ProvisionInput.Builder inputBuilder = ProvisionInput.builder().amount(amount);
        String selectedPool = configuredPoolId();
        if (allDevices) {
            int total = 0;
            QueryResults<DeviceObject> results = null;
            do {
                results = cli.execute(service.listDevices(selectedPool, QueryParams.builder()
                        .limit(100)
                        .nextToken(Optional.ofNullable(results)
                                .map(QueryResults::nextToken)
                                .orElse(null))
                        .build()));
                total += results.results().size();
            } while (results.isTruncated());
            inputBuilder.amount(total);
        }
        pool.provisionSync(inputBuilder.build(), provisionTimeout, TimeUnit.SECONDS).forEach(thunk.andThen(device -> {
            cli.executeAndPrint(service.releaseDeviceLock(selectedPool, device.id()));
        }));
    }

    @CommandLine.Command(
            name = "execute",
            description = "Runs an arbitrary command on the devices",
            scope = CommandLine.ScopeType.INHERIT,
            subcommands = {CommandLine.HelpCommand.class}
    )
    public void execute(
            @CommandLine.Option(names = "--timeout", defaultValue = "30") long timeout,
            @CommandLine.Parameters(paramLabel = "ARG") String[] args
    ) {
        Objects.requireNonNull(args, "<ARGS> are required");
        final DeviceLabService service = cli.createService();
        DevicePool pool = createPool(service);
        provisionOnFrom(pool, service, device -> {
            System.out.println("Executing on device: " + device.id());
            CommandOutput output = device.execute(CommandInput.builder()
                    .timeout(Duration.ofSeconds(timeout))
                    .line(args[0])
                    .args(Arrays.stream(args)
                            .skip(1)
                            .collect(Collectors.toList()))
                    .build());
            System.out.println(output.toUTF8String());
        });
    }

    @CommandLine.Command(
            name = "cp",
            description = "Copies files to and from the local machine",
            scope = CommandLine.ScopeType.INHERIT,
            subcommands = {CommandLine.HelpCommand.class}
    )
    public void copy(
            @CommandLine.Parameters(
                    paramLabel = "src",
                    description = "source of the copy",
                    index = "0"
            ) String source,
            @CommandLine.Parameters(
                    paramLabel = "des",
                    description = "destination of the copy",
                    index = "1"
            ) String destination,
            @CommandLine.Option(names = {"-r", "--recursive"}) boolean recursive
    ) {
        CopyInput.Builder builder = CopyInput.builder();
        if (recursive) {
            builder.addOptions(CopyOption.RECURSIVE);
        }
        Function<Device, String> destinationFunction;
        BiConsumer<Device, CopyInput> copy;
        if (source.startsWith(REMOTE_PREFIX)) {
            builder.source(source.substring(REMOTE_PREFIX.length()));
            destinationFunction = device -> {
                String[] parts = destination.split("/");
                parts[parts.length - 1] = device.id() + "." + parts[parts.length - 1];
                return String.join("/", parts);
            };
            copy = Device::copyFrom;
        } else if (destination.startsWith(REMOTE_PREFIX)) {
            builder.source(source);
            destinationFunction = device -> destination.substring(REMOTE_PREFIX.length());
            copy = Device::copyTo;
        } else {
            throw new IllegalArgumentException("source or destination must prefix with 'remote:' for device location");
        }
        final DeviceLabService service = cli.createService();
        DevicePool pool = createPool(service);
        provisionOnFrom(pool, service, device -> {
            CopyInput input = builder.destination(destinationFunction.apply(device)).build();
            System.out.println(device.id() + ": Copying " + input.source() + " to " + input.destination());
            copy.accept(device, input);
        });
    }
}
