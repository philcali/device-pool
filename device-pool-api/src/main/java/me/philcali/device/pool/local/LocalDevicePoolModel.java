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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A local implementation of a {@link DevicePool}. The intention behind this implementation is to
 * facilitate client code integration without requiring a network, ie unit tests. The entire {@link LocalDevicePool}
 * is a wrapper around a provision request cache and {@link LocalDevice} creation. The {@link LocalDevice}
 * implementations are wrappers around the {@link Process} and {@link Files} APIs.
 */
@ApiModel
@Value.Immutable
abstract class LocalDevicePoolModel implements DevicePool, FileMixin {
    private final AtomicInteger incrementingId = new AtomicInteger();
    private final Map<String, ProvisionOutput> provisions = new ConcurrentHashMap<>();

    /**
     * <p>baseDirectory.</p>
     *
     * @return a {@link java.nio.file.Path} object
     */
    @Value.Default
    public Path baseDirectory() {
        try {
            return Files.createTempDirectory("local-pool-");
        } catch (IOException e) {
            throw new ProvisioningException(e);
        }
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link me.philcali.device.pool.local.LocalDevicePool} object
     */
    public static LocalDevicePool create() {
        return LocalDevicePool.builder().build();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException {
        return Optional.ofNullable(provisions.get(output.id()))
                .orElseThrow(() -> new ProvisioningException("Could not find a provision id " + output.id()));
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public void close() {
        DevicePool.super.close();
        provisions.clear();
        try {
            cleanUp();
        } catch (IOException e) {
            throw new ProvisioningException(e);
        }
    }
}
