/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.api.model.DevicePoolLockOptions;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
import me.philcali.device.pool.service.api.model.UpdateLockObject;
import me.philcali.device.pool.service.client.AwsV4SigningInterceptor;
import me.philcali.device.pool.service.client.DeviceLabService;
import picocli.CommandLine;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * <p>DeviceLabCLI class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "device-lab",
        version = "1.1.2-SNAPSHOT",
        description = "Device Lab CLI for the control plane",
        subcommands = {CommandLine.HelpCommand.class, Devices.class}
)
public class DeviceLabCLI {
    static final String DEFAULT_LIMIT = "100";

    @CommandLine.Option(
            names = "--endpoint",
            description = "Endpoint override",
            scope = CommandLine.ScopeType.INHERIT
    )
    String endpoint;

    @CommandLine.Option(
            names = "--properties-file",
            description = "Static properties file location",
            scope = CommandLine.ScopeType.INHERIT
    )
    Path propertiesFile;

    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Verbose flag to output wire details",
            scope = CommandLine.ScopeType.INHERIT
    )
    boolean verbose;

    @CommandLine.Option(
            names = {"-V", "--version"},
            description = "Displays the version of device-lab",
            versionHelp = true
    )
    boolean version;

    final ObjectMapper mapper;

    private Optional<DevicePoolConfig> loadedConfig;

    private DeviceLabCLI() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects
     */
    public static void main(String[] args) {
        System.exit(new CommandLine(new DeviceLabCLI()).execute(args));
    }

    private Path defaultConfigPath() {
        return Path.of(System.getProperty("user.home")).resolve(".device-lab").resolve("config.properties");
    }

    protected Optional<DevicePoolConfig> loadConfig() {
        if (Objects.isNull(loadedConfig)) {
            DevicePoolConfig config = null;
            Path configPath = Optional.ofNullable(propertiesFile).orElseGet(this::defaultConfigPath);
            if (Files.exists(configPath)) {
                try (InputStream inputStream = Files.newInputStream(configPath)) {
                    config = DevicePoolConfigProperties.load(inputStream);
                } catch (IOException ie) {
                    throw new RuntimeException(ie);
                }
            }
            loadedConfig = Optional.ofNullable(config);
        }
        return loadedConfig;
    }

    protected DeviceLabService createService() {
        final String serviceEndpoint = Optional.ofNullable(endpoint)
                .orElseGet(() -> loadConfig()
                        .flatMap(config -> config.namespace("provision.lab"))
                        .flatMap(entry -> entry.get("endpoint"))
                        .orElseThrow(() -> new IllegalStateException("Requires an endpoint or configured properties")));
        return DeviceLabService.create((client, builder) -> {
            client.addInterceptor(AwsV4SigningInterceptor.create());
            builder.baseUrl(serviceEndpoint);
        });
    }

    protected <T> T execute(Call<T> call) {
        if (verbose) {
            System.out.println("Request URL: " + call.request().url().uri());
            System.out.println("Request Method: " + call.request().method());
        }
        try {
            Response<T> response = call.execute();
            if (verbose) {
                System.out.println("Response Status: " + response.code());
                System.out.println("Response Headers: ");
                System.out.println(response.headers());
            }
            if (Objects.nonNull(response.errorBody())) {
                throw new RuntimeException(response.errorBody().string());
            }
            return response.body();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void executeAndPrint(Call<?> call) {
        Object result = execute(call);
        if (Objects.isNull(result)) {
            System.out.println("OK");
        } else {
            try {
                mapper.writeValue(System.out, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @CommandLine.Command(
            name = "configure",
            description = "Configure the CLI client locally"
    )
    public void configure() throws IOException {
        Path configPath = defaultConfigPath();
        Files.createDirectories(configPath.getParent());
        Properties properties = new Properties();
        // TODO: clean this up ... interact with DevicePoolConfig instead of Properties directly
        // Default to base pool
        properties.setProperty("device.pool.class", BaseDevicePool.class.getName());
        if (Files.exists(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
            }
        }
        Arrays.asList("endpoint", "poolId", "platform", "port").forEach(entry -> {
            String value = System.console().readLine("Enter the device lab %s [%s]: %n",
                    entry, properties.getProperty("device.pool.provision.lab." + entry, ""));
            if (entry.equals("port")) {
                int numericPort = Integer.parseInt(properties.getProperty("provision.lab.port", "22"));
                if (!value.trim().isEmpty()) {
                    numericPort = Integer.parseInt(value);
                }
                value = Integer.toString(numericPort);
            } else if (value.trim().isEmpty()) {
                // If using existing, allow pass through
                value = properties.getProperty("device.pool.provision.lab." + entry, value);
            }
            properties.setProperty("device.pool.provision.lab." + entry, value);
        });
        try (OutputStream outputStream = Files.newOutputStream(configPath)) {
            properties.store(outputStream, "Auto-generated via configure");
        }
    }

    /**
     * <p>listDevicePools.</p>
     *
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     */
    @CommandLine.Command(
            name = "list-device-pools",
            description = "List device pools"
    )
    public void listDevicePools(
            @CommandLine.Option(names = "--next-token") String nextToken,
            @CommandLine.Option(names = "--limit", defaultValue = DEFAULT_LIMIT) Integer limit
    ) {
       executeAndPrint(createService().listDevicePools(nextToken, limit));
    }

    /**
     * <p>listDevices.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     */
    @CommandLine.Command(
            name = "list-devices",
            description = "List devices to device pools"
    )
    public void listDevices(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--next-token") String nextToken,
            @CommandLine.Option(names = "--limit", defaultValue = DEFAULT_LIMIT) Integer limit
    ) {
        executeAndPrint(createService().listDevices(poolId, nextToken, limit));
    }

    /**
     * <p>listProvisions.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     */
    @CommandLine.Command(
            name = "list-provisions",
            description = "List provision requests to device pools"
    )
    public void listProvisions(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--next-token") String nextToken,
            @CommandLine.Option(names = "--limit", defaultValue = DEFAULT_LIMIT) Integer limit
    ) {
        executeAndPrint(createService().listProvisions(poolId, nextToken, limit));
    }

    /**
     * <p>listReservations.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     */
    @CommandLine.Command(
            name = "list-reservations",
            description = "List device reservation requests to provisions"
    )
    public void listReservations(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--provision-id", required = true) String provisionId,
            @CommandLine.Option(names = "--next-token") String nextToken,
            @CommandLine.Option(names = "--limit", defaultValue = DEFAULT_LIMIT) Integer limit
    ) {
        executeAndPrint(createService().listReservations(poolId, provisionId, nextToken, limit));
    }

    /**
     * <p>getDevicePool.</p>
     *
     * @param poolId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "get-device-pool",
            description = "Obtains a single device pool metadata"
    )
    public void getDevicePool(
            @CommandLine.Option(names = "--pool-id") String poolId
    ) {
        executeAndPrint(createService().getDevicePool(poolId));
    }

    /**
     * <p>getDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "get-device",
            description = "Obtains a single device metadata"
    )
    public void getDevice(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId
    ) {
        executeAndPrint(createService().getDevice(poolId, deviceId));
    }

    /**
     * <p>getProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "get-provision",
            description = "Obtains a single provision request metadata"
    )
    public void getProvision(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--provision-id", required = true) String provisionId
    ) {
        executeAndPrint(createService().getProvision(poolId, provisionId));
    }

    /**
     * <p>getReservation.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param reservationId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "get-reservation",
            description = "Obtains a single device reservation"
    )
    public void getReservation(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--provision-id", required = true) String provisionId,
            @CommandLine.Option(names = "--reservation-id", required = true) String reservationId
    ) {
        executeAndPrint(createService().getReservation(poolId, provisionId, reservationId));
    }

    /**
     * <p>createDevicePool.</p>
     *
     * @param name a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param type a {@link java.lang.String} object
     * @param endpointType a {@link java.lang.String} object
     * @param endpointUri a {@link java.lang.String} object
     * @param duration a {@link java.time.Duration} object
     * @param enabled a boolean
     */
    @CommandLine.Command(
            name = "create-device-pool",
            description = "Creates a single device pool"
    )
    public void createDevicePool(
            @CommandLine.Option(names = "--pool-id", required = true) String name,
            @CommandLine.Option(names = "--description") String description,
            @CommandLine.Option(names = "--type", defaultValue = "MANAGED") String type,
            @CommandLine.Option(names = "--endpoint-type") String endpointType,
            @CommandLine.Option(names = "--endpoint-uri") String endpointUri,
            @CommandLine.Option(names = "--lock-options-duration") Duration duration,
            @CommandLine.Option(names = "--lock-options-enabled") boolean enabled
    ) {
        DevicePoolEndpoint endpoint = null;
        if (Objects.nonNull(endpointUri) || Objects.nonNull(endpointType)) {
            endpoint = DevicePoolEndpoint.builder()
                    .uri(endpointUri)
                    .type(DevicePoolEndpointType.valueOf(endpointUri))
                    .build();
        }
        DevicePoolLockOptions.Builder lockOptionsBuilder = DevicePoolLockOptions.builder()
                .enabled(enabled);
        if (Objects.nonNull(duration)) {
            lockOptionsBuilder.duration(duration.toSeconds());
        }
        executeAndPrint(createService().createDevicePool(CreateDevicePoolObject.builder()
                .type(DevicePoolType.valueOf(type.toUpperCase()))
                .description(description)
                .endpoint(endpoint)
                .lockOptions(lockOptionsBuilder.build())
                .name(name)
                .build()));
    }

    /**
     * <p>createDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param publicAddress a {@link java.lang.String} object
     * @param privateAddress a {@link java.lang.String} object
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "create-device",
            description = "Creates a single device for a device pool"
    )
    public void createDevice(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId,
            @CommandLine.Option(names = "--public-address", required = true) String publicAddress,
            @CommandLine.Option(names = "--private-address") String privateAddress,
            @CommandLine.Option(names = "--expires-in") Instant expiresIn
    ) {
        executeAndPrint(createService().createDevice(poolId, CreateDeviceObject.builder()
                .id(deviceId)
                .publicAddress(publicAddress)
                .privateAddress(privateAddress)
                .expiresIn(expiresIn)
                .build()));
    }

    /**
     * <p>createProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param id a {@link java.lang.String} object
     * @param amount a int
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "create-provision",
            description = "Creates a single provision request"
    )
    public void createProvision(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--id", required = true) String id,
            @CommandLine.Option(names = "--amount", defaultValue = "1") int amount,
            @CommandLine.Option(names = "--expires-in") Instant expiresIn
    ) {
        executeAndPrint(createService().createProvision(poolId, CreateProvisionObject.builder()
                .id(id)
                .amount(amount)
                .expiresIn(expiresIn)
                .build()));
    }

    /**
     * <p>updateDevicePool.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param description a {@link java.lang.String} object
     * @param type a {@link java.lang.String} object
     * @param endpointType a {@link java.lang.String} object
     * @param endpointUri a {@link java.lang.String} object
     * @param duration a {@link java.time.Duration} object
     * @param enabled a {@link java.lang.Boolean} object
     */
    @CommandLine.Command(
            name = "update-device-pool",
            description = "Updates a single device pool record"
    )
    public void updateDevicePool(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--description") String description,
            @CommandLine.Option(names = "--type") String type,
            @CommandLine.Option(names = "--endpoint-type") String endpointType,
            @CommandLine.Option(names = "--endpoint-uri") String endpointUri,
            @CommandLine.Option(names = "--lock-options-duration") Duration duration,
            @CommandLine.Option(names = "--lock-options-enabled") Boolean enabled
    ) {
        DevicePoolEndpoint endpoint = null;
        if (Objects.nonNull(endpointUri) || Objects.nonNull(endpointType)) {
            endpoint = DevicePoolEndpoint.builder()
                    .uri(endpointUri)
                    .type(DevicePoolEndpointType.valueOf(endpointUri))
                    .build();
        }
        DevicePoolLockOptions lockOptions = null;
        if (Objects.nonNull(enabled)) {
            DevicePoolLockOptions.Builder builder = DevicePoolLockOptions.builder().enabled(enabled);
            if (Objects.nonNull(duration)) {
                builder.duration(duration.toSeconds());
            }
            lockOptions = builder.build();
        }
        executeAndPrint(createService().updateDevicePool(poolId, UpdateDevicePoolObject.builder()
                .description(description)
                .type(Optional.ofNullable(type).map(DevicePoolType::valueOf).orElse(null))
                .lockOptions(lockOptions)
                .endpoint(endpoint)
                .build()));
    }

    /**
     * <p>updateDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param publicAddress a {@link java.lang.String} object
     * @param privateAddress a {@link java.lang.String} object
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "update-device",
            description = "Updates a single device metadata record"
    )
    public void updateDevice(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId,
            @CommandLine.Option(names = "--public-address") String publicAddress,
            @CommandLine.Option(names = "--private-address") String privateAddress,
            @CommandLine.Option(names = "--expires-in") Instant expiresIn
    ) {
        executeAndPrint(createService().updateDevice(poolId, deviceId, UpdateDeviceObject.builder()
                .publicAddress(publicAddress)
                .privateAddress(privateAddress)
                .expiresIn(expiresIn)
                .build()));
    }

    /**
     * <p>cancelProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "cancel-provision",
            description = "Cancel a single non-terminal provision request"
    )
    public void cancelProvision(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--provision-id", required = true) String provisionId
    ) {
        executeAndPrint(createService().cancelProvision(poolId, provisionId));
    }

    /**
     * <p>deleteProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "delete-provision",
            description = "Deletes a single terminal provision request"
    )
    public void deleteProvision(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--provision-id", required = true) String provisionId
    ) {
        executeAndPrint(createService().deleteProvision(poolId, provisionId));
    }

    /**
     * <p>cancelReservation.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param reservationId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "cancel-reservation",
            description = "Cancels a single non-terminal device reservation"
    )
    public void cancelReservation(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--provision-id", required = true) String provisionId,
            @CommandLine.Option(names = "--reservation-id", required = true) String reservationId
    ) {
        executeAndPrint(createService().cancelReservation(poolId, provisionId, reservationId));
    }

    /**
     * <p>createDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param holder a {@link java.lang.String} object
     * @param duration a {@link java.time.Duration} object
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "create-device-pool-lock",
            description = "Creates a lock on a single device pool"
    )
    public void createDevicePoolLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--holder", required = true) String holder,
            @CommandLine.Option(names = "--duration") Duration duration,
            @CommandLine.Option(names = "--expires-in") Instant expiresIn
    ) {
        CreateLockObject.Builder builder = CreateLockObject.builder().holder(holder);
        if (Objects.nonNull(duration)) {
            builder.duration(duration);
        }
        if (Objects.nonNull(expiresIn)) {
            builder.expiresIn(expiresIn);
        }
        executeAndPrint(createService().createDevicePoolLock(poolId, builder.build()));
    }

    /**
     * <p>createDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param holder a {@link java.lang.String} object
     * @param duration a {@link java.time.Duration} object
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "create-device-lock",
            description = "Creates a lock on a single device"
    )
    public void createDeviceLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId,
            @CommandLine.Option(names = "--holder", required = true) String holder,
            @CommandLine.Option(names = "--duration") Duration duration,
            @CommandLine.Option(names = "--expires-in") Instant expiresIn
    ) {
        CreateLockObject.Builder builder = CreateLockObject.builder().holder(holder);
        if (Objects.nonNull(duration)) {
            builder.duration(duration);
        }
        if (Objects.nonNull(expiresIn)) {
            builder.expiresIn(expiresIn);
        }
        executeAndPrint(createService().createDeviceLock(poolId, deviceId, builder.build()));
    }

    /**
     * <p>getDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "get-device-pool-lock",
            description = "Obtains lock metadata on a single pool"
    )
    public void getDevicePoolLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId
    ) {
        executeAndPrint(createService().getDevicePoolLock(poolId));
    }

    /**
     * <p>getDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "get-device-lock",
            description = "Obtains lock metadata on a single device"
    )
    public void getDeviceLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId
    ) {
        executeAndPrint(createService().getDeviceLock(poolId, deviceId));
    }

    /**
     * <p>extendDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param holder a {@link java.lang.String} object
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "extend-device-pool-lock",
            description = "Extends a lock on a single device pool"
    )
    public void extendDevicePoolLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--holder", required = true) String holder,
            @CommandLine.Option(names = "--expires-in", required = true) Instant expiresIn
    ) {
        UpdateLockObject.Builder builder = UpdateLockObject.builder().holder(holder).expiresIn(expiresIn);
        executeAndPrint(createService().extendDevicePoolLock(poolId, builder.build()));
    }

    /**
     * <p>extendDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param holder a {@link java.lang.String} object
     * @param expiresIn a {@link java.time.Instant} object
     */
    @CommandLine.Command(
            name = "extend-device-lock",
            description = "Extends a lock on a single device"
    )
    public void extendDeviceLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId,
            @CommandLine.Option(names = "--holder", required = true) String holder,
            @CommandLine.Option(names = "--expires-in", required = true) Instant expiresIn
    ) {
        executeAndPrint(createService().extendDeviceLock(poolId, deviceId, UpdateLockObject.builder()
                .holder(holder)
                .expiresIn(expiresIn)
                .build()));
    }

    /**
     * <p>releaseDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "release-device-pool-lock",
            description = "Forcibly releases a lock held on a single pool"
    )
    public void releaseDevicePoolLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId
    ) {
        executeAndPrint(createService().releaseDevicePoolLock(poolId));
    }

    /**
     * <p>releaseDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "release-device-lock",
            description = "Forcibly releases a lock held on a single device"
    )
    public void releaseDeviceLock(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId,
            @CommandLine.Option(names = "--device-id", required = true) String deviceId
    ) {
        executeAndPrint(createService().releaseDeviceLock(poolId, deviceId));
    }

    /**
     * <p>deleteDevicePool.</p>
     *
     * @param poolId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "delete-device-pool",
            description = "Deletes a single device pool and all associated data"
    )
    public void deleteDevicePool(
            @CommandLine.Option(names = "--pool-id", required = true) String poolId
    ) {
        executeAndPrint(createService().deleteDevicePool(poolId));
    }

    /**
     * <p>deleteDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     */
    @CommandLine.Command(
            name = "delete-device",
            description = "Deletes a single device on a device pool"
    )
    public void deleteDevice(
            @CommandLine.Option(
                    names = "--pool-id", required = true,
                    paramLabel = "poolId",
                    description = "Device pool identifier"
            ) String poolId,
            @CommandLine.Option(
                    names = "--device-id", required = true,
                    paramLabel = "deviceId",
                    description = "Device metadata identifier"
            ) String deviceId
    ) {
        executeAndPrint(createService().deleteDevice(poolId, deviceId));
    }
}
