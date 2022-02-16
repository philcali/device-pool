/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.local;

import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApiModel
@Value.Immutable
abstract class LocalDevicePoolModel implements DevicePool {
    private final AtomicInteger incrementingId = new AtomicInteger();
    private final Map<String, ProvisionOutput> provisions = new ConcurrentHashMap<>();

    @Value.Default
    Path baseDirectory() {
        try {
            return Files.createTempDirectory("local-pool-");
        } catch (IOException e) {
            throw new ProvisioningException(e);
        }
    }

    public static LocalDevicePool create() {
        return LocalDevicePool.builder().build();
    }

    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        return provisions.computeIfAbsent(input.id(), id -> ProvisionOutput.builder()
                .id(id)
                .status(Status.SUCCEEDED)
                .addAllReservations(IntStream.range(0, input.amount())
                        .mapToObj(index -> Reservation.builder()
                                .deviceId("host-" + incrementingId.incrementAndGet())
                                .status(Status.SUCCEEDED)
                                .build())
                        .collect(Collectors.toList()))
                .build());
    }

    @Override
    public ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException {
        return Optional.ofNullable(provisions.get(output.id()))
                .orElseThrow(() -> new ProvisioningException("Could not find a provision id " + output.id()));
    }

    @Override
    public List<Device> obtain(ProvisionOutput output) throws ProvisioningException {
        List<Device> devices = new ArrayList<>();
        for (Reservation reservation : output.reservations()) {
            Path hostDirectory = baseDirectory().resolve(reservation.deviceId());
            try {
                if (Files.notExists(hostDirectory)) {
                    Files.createDirectory(hostDirectory);
                }
            } catch (IOException e) {
                throw new ProvisioningException(e);
            }
            devices.add(LocalDevice.of(hostDirectory, reservation.deviceId()));
        }
        return Collections.unmodifiableList(devices);
    }

    @Override
    public void close() {
        DevicePool.super.close();
        try (Stream<Path> stream = Files.walk(baseDirectory())) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new ProvisioningException(e);
        }
    }
}
