/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.Device;
import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.reservation.ReservationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This concrete {@link me.philcali.device.pool.provision.ProvisionService} facilities in-memory provisioning, thus called the
 * {@link me.philcali.device.pool.provision.LocalProvisionService}. This {@link me.philcali.device.pool.provision.ProvisionService} cannot be reliably used in
 * distributed systems unless decorated with the implementation of a {@link me.philcali.device.pool.provision.LockingProvisionService}.
 * The provision method is asynchronous, meaning it will never return a complete {@link me.philcali.device.pool.model.ProvisionOutput}
 * for the initial request. The describe method can be called repeatedly.
 */
@APIShadowModel
@Value.Immutable
public abstract class LocalProvisionService implements ProvisionService, ReservationService {
    private static final Logger LOGGER = LogManager.getLogger(LocalProvisionService.class);
    private final Map<String, CachedEntry<ProvisionOutput>> reservations = new ConcurrentHashMap<>();
    private final BlockingQueue<LocalProvisionEntry> activeProvisions = new LinkedBlockingQueue<>();
    private final BlockingQueue<Host> availableHosts = new LinkedBlockingQueue<>();
    private final LocalProvisionRunnable currentRunnable = new LocalProvisionRunnable();
    private final LocalProvisionReaper reapRunnable = new LocalProvisionReaper();
    private final Lock lock = new ReentrantLock();

    abstract Set<Host> hosts();

    @Value.Default
    boolean expireProvisions() {
        return true;
    }

    @Value.Default
    long provisionTimeout() {
        return TimeUnit.HOURS.toMillis(1);
    }

    @Value.Default
    ExecutorService executorService() {
        int threads = 1;
        if (expireProvisions()) {
            threads += 1;
        }
        return Executors.newFixedThreadPool(threads, runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("Local-Provisioning");
            return thread;
        });
    }

    /**
     * Convenience method for constructing a fluent builder.
     *
     * @return {@link me.philcali.device.pool.provision.LocalProvisionService.Builder}
     */
    public static Builder builder() {
        return new LocalProvisionService.Builder();
    }

    public static final class Builder
            extends ImmutableLocalProvisionService.Builder
            implements ConfigBuilder<LocalProvisionService> {

        @Override
        public LocalProvisionService fromConfig(DevicePoolConfig config) {
            return config.namespace("provision.local").flatMap(entry -> {
                entry.get("timeout").map(Long::parseLong).ifPresent(this::provisionTimeout);
                entry.get("expires").map(Boolean::parseBoolean).ifPresent(this::expireProvisions);
                return Optional.ofNullable(entry.properties().get("hosts")).map(hosts -> {
                    hosts.properties().keySet().forEach(hostId -> {
                        DevicePoolConfig.DevicePoolConfigEntry hostEntry = hosts.properties().get(hostId);
                        addHosts(Host.builder()
                                .deviceId(hostEntry.get("id").orElse(hostId))
                                .port(hostEntry.get("port").map(Integer::parseInt).orElse(22))
                                .proxyJump(hostEntry.get("proxy").orElse(null))
                                .hostName(hostEntry.get("address")
                                        .orElseThrow(() -> new ProvisioningException("Host entry "
                                                + hostId + " does not have an endpoint")))
                                .platform(hostEntry.get("platform")
                                        .map(PlatformOS::fromString)
                                        .orElseThrow(() -> new ProvisioningException("Host entry "
                                                + hostId + " does not have a platform")))
                                .build());
                    });
                    return build();
                });
            }).orElseThrow(() -> new ProvisioningException("Please configure device.pool.provision.local with hosts"));
        }
    }

    @Value.Check
    LocalProvisionService validate() {
        if (hosts().isEmpty()) {
            throw new IllegalArgumentException("hosts must contain at least one entry");
        }
        // Initialize the available hosts from the set of hosts
        if (!hosts().stream().allMatch(availableHosts::offer)) {
            throw new IllegalStateException("could not queue pending hosts");
        }
        // Start the background queue drain, to supply provisions
        executorService().execute(currentRunnable);
        // Start provision expiry
        if (expireProvisions()) {
            executorService().execute(reapRunnable);
        }
        return this;
    }

    private static class CachedEntry<T> {
        T value;
        long expiresIn;

        CachedEntry(T value, long expiresIn) {
            this.value = value;
            this.expiresIn = expiresIn;
        }
    }

    private static class LocalProvisionEntry {
        ProvisionInput input;
        ProvisionOutput output;

        LocalProvisionEntry(ProvisionInput input, ProvisionOutput output) {
            this.input = input;
            this.output = output;
        }
    }

    private class LocalProvisionReaper implements Runnable {
        volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                try {
                    int amount = LocalProvisionService.this.releaseAvailable(System.currentTimeMillis());
                    LOGGER.debug("Reaped {} devices", amount);
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    LOGGER.info("Reaper is shutting down.");
                    running = false;
                }
            }
        }
    }

    private class LocalProvisionRunnable implements Runnable {
        volatile boolean running = true;

        @Override
        public void run() {
            try {
                while (running) {
                    final LocalProvisionEntry entry = activeProvisions.take();
                    final CachedEntry<ProvisionOutput> existing = reservations.computeIfPresent(entry.input.id(),
                            (id, cache) -> new CachedEntry<>(ProvisionOutput.builder()
                                    .from(cache.value)
                                    .status(Status.PROVISIONING)
                                    .build(), cache.expiresIn));
                    if (Objects.isNull(existing)) {
                        LOGGER.info("Provision {} is no longer active", entry.input.id());
                        continue;
                    }
                    for (int i = 1; i <= entry.input.amount(); i++) {
                        final Host host = availableHosts.take();
                        // Adding a host to a provision is atomic, complete with checks of existence
                        lock.lock();
                        try {
                            // Provision was swept or released before host was obtained, recycle host and break
                            if (!reservations.containsKey(entry.input.id())) {
                                LOGGER.warn("Host {} was applied to provision that no longer exists {}",
                                        host.deviceId(),
                                        entry.input.id());
                                availableHosts.offer(host);
                                break;
                            }
                            final Status status = i == entry.input.amount() ? Status.SUCCEEDED : Status.PROVISIONING;
                            reservations.computeIfPresent(entry.input.id(), (id, cache) -> new CachedEntry<>(
                                    ProvisionOutput.builder()
                                            .from(cache.value)
                                            .status(status)
                                            .addReservations(Reservation.of(host.deviceId(), Status.SUCCEEDED))
                                            .build(), cache.expiresIn));
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            } catch (InterruptedException ie) {
                LOGGER.info("Queue poll interrupted, stopping");
                running = false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        final CachedEntry<ProvisionOutput> handle = reservations.computeIfAbsent(input.id(), id -> new CachedEntry<>(
                ProvisionOutput.builder()
                    .id(id)
                    .status(Status.REQUESTED)
                    .build(), System.currentTimeMillis() + provisionTimeout()));
        if (!activeProvisions.offer(new LocalProvisionEntry(input, handle.value))) {
            throw new ProvisioningException("Could not create a provision with id: " + input.id());
        }
        return handle.value;
    }

    /** {@inheritDoc} */
    @Override
    public ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException {
        CachedEntry<ProvisionOutput> handle = reservations.get(output.id());
        if (Objects.isNull(handle)) {
            throw new ProvisioningException("Could not find a provision with id: " + output.id());
        }
        return handle.value;
    }

    /** {@inheritDoc} */
    @Override
    public Host exchange(Reservation reservation) throws ReservationException {
        return hosts().stream()
                .filter(h -> h.deviceId().equals(reservation.deviceId()))
                .findFirst()
                .orElseThrow(() -> new ReservationException("Could not a host with id: " + reservation.deviceId()));
    }

    private boolean releaseHost(String deviceId) {
        return hosts().stream()
                .filter(host -> host.deviceId().equals(deviceId))
                .filter(host -> !availableHosts.contains(host))
                .reduce(false,
                        (left, right) -> availableHosts.offer(right) || left,
                        (left, right) -> left || right);
    }

    /**
     * <p>release.</p>
     *
     * @param device a {@link me.philcali.device.pool.Device} object
     * @return a boolean
     */
    public boolean release(Device device) {
        return releaseHost(device.id());
    }

    /**
     * <p>release.</p>
     *
     * @param output a {@link me.philcali.device.pool.model.ProvisionOutput} object
     * @return a int
     */
    public int release(ProvisionOutput output) {
        AtomicInteger released = new AtomicInteger();
        CachedEntry<ProvisionOutput> handle = reservations.remove(output.id());
        if (Objects.nonNull(handle)) {
            handle.value.reservations().stream()
                    .map(Reservation::deviceId)
                    .filter(this::releaseHost)
                    .forEach(hostId -> {
                        LOGGER.debug("Release host {}", hostId);
                        released.incrementAndGet();
                    });
            LOGGER.info("Released provision with id: {}", output.id());
        }
        return released.get();
    }

    /**
     * <p>releaseAvailable.</p>
     *
     * @param when a long
     * @return a int
     */
    protected int releaseAvailable(long when) {
        lock.lock();
        try {
            int released = 0;
            for (CachedEntry<ProvisionOutput> cachedEntry : reservations.values()) {
                if (cachedEntry.expiresIn < when) {
                    released += release(cachedEntry.value);
                }
            }
            return released;
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>extend.</p>
     *
     * @param output a {@link me.philcali.device.pool.model.ProvisionOutput} object
     */
    public void extend(ProvisionOutput output) {
        lock.lock();
        reservations.computeIfPresent(output.id(), (id, cache) -> {
            cache.expiresIn += provisionTimeout();
            return cache;
        });
        lock.unlock();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws Exception {
        currentRunnable.running = false;
        reapRunnable.running = false;
    }
}
