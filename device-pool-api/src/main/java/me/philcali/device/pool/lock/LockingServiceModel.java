/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.lock;

import me.philcali.device.pool.exceptions.LockingConflictException;
import me.philcali.device.pool.exceptions.LockingException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@link me.philcali.device.pool.lock.LockingService} wraps the {@link me.philcali.device.pool.lock.LockingMechanism} for automatic lock extension
 * for the duration of a longer running process. The component is a convenience that simply
 * tracks all of the {@link me.philcali.device.pool.model.LockOutput}s acquired by a client to the {@link me.philcali.device.pool.lock.LockingMechanism}.
 * The service is useful to lock control plane resources for whatever reason. The typical use
 * case is:
 * <br>
 * <pre>
 *     LockingService locker = LockingService.of(lockingMechanism);
 *     try (LockingService.Lock lock = locker.tryAcquire(LockInput.of(id)) {
 *         // Do something with held resource
 *     }
 * </pre>
 */
@ApiModel
@Value.Immutable
abstract class LockingServiceModel implements AutoCloseable {
    /** Constant <code>LOGGER</code> */
    protected static final Logger LOGGER = LogManager.getLogger(LockingService.class);
    protected final Map<String, Lock> activeLocks = new ConcurrentHashMap<>();
    protected ScheduledFuture<Void> extensionSchedule;

    @Value.Default
    boolean manageActiveLocks() {
        return true;
    }

    abstract LockingMechanism mechanism();

    @Value.Default
    long extensionInterval() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Value.Default
    ScheduledExecutorService executorService() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("Extend-Locks");
            return thread;
        });
    }

    @Value.Check
    @SuppressWarnings("unchecked")
    LockingServiceModel normalize() {
        if (manageActiveLocks()) {
            LockingService copy = LockingService.copyOf(this);
            Runnable extension = new LockExtensionRunnable(copy);
            copy.extensionSchedule = (ScheduledFuture<Void>) executorService()
                    .scheduleWithFixedDelay(extension, 0, extensionInterval(), TimeUnit.MILLISECONDS);
            return copy;
        }
        return this;
    }

    @FunctionalInterface
    private interface FutureThunk<I, R> {
        R fulfill(CompletableFuture<I> input) throws ExecutionException, InterruptedException, TimeoutException;
    }

    private Lock lock(LockInput input, FutureThunk<LockOutput, LockOutput> thunk) throws InterruptedException {
        try {
            LockOutput output = thunk.fulfill(mechanism().lock(input));
            return activeLocks.compute(input.id(), (id, original) -> new Lock(input, output));
        } catch (ExecutionException | TimeoutException e) {
            throw new LockingException(e);
        }
    }

    /**
     * Tries to acquire a lock within a set duration.
     *
     * @param input Information to hold the lock in the form of a {@link me.philcali.device.pool.model.LockInput}
     * @param amount The amount of {@link java.util.concurrent.TimeUnit} to block
     * @param unit The {@link java.util.concurrent.TimeUnit} value to apply to the wait
     * @return The {@link me.philcali.device.pool.lock.LockingServiceModel.Lock} tracking the {@link me.philcali.device.pool.model.LockInput} and resulting {@link me.philcali.device.pool.model.LockOutput}
     * @throws java.lang.InterruptedException Thrown when interrupting lock acquisition
     */
    public Lock tryAcquire(LockInput input, long amount, TimeUnit unit) throws InterruptedException {
        return lock(input, future -> future.get(amount, unit));
    }

    /**
     * Tries to acquire a lock in the form of a {@link me.philcali.device.pool.model.LockInput}, indefinitely.
     *
     * @param input Information to hold the lock in the form of a {@link me.philcali.device.pool.model.LockInput}
     * @return The {@link me.philcali.device.pool.lock.LockingServiceModel.Lock} tracking the {@link me.philcali.device.pool.model.LockInput} and resulting {@link me.philcali.device.pool.model.LockOutput}
     * @throws java.lang.InterruptedException Thrown if the thread is interrupted waiting to acquire a lock
     */
    public Lock tryAcquire(LockInput input) throws InterruptedException {
        return lock(input, CompletableFuture::get);
    }

    public class Lock implements AutoCloseable {
        private final LockInput input;
        private volatile LockOutput output;

        public Lock(LockInput input, LockOutput output) {
            this.input = input;
            this.output = output;
        }

        public LockInput lockInput() {
            return input;
        }

        public boolean isExpired() {
            return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) > output.expiresIn();
        }

        @Override
        public String toString() {
            return "Lock{" +
                    "input=" + input +
                    ", output=" + output +
                    '}';
        }

        @Override
        public void close() {
            LockingServiceModel.this.release(this);
        }
    }

    private static class LockExtensionRunnable implements Runnable {
        private final LockingService service;

        public LockExtensionRunnable(LockingService service) {
            this.service = service;
        }

        @Override
        public void run() {
            for (Lock lock : service.activeLocks.values()) {
                try {
                    if (lock.isExpired()) {
                        service.activeLocks.remove(lock.lockInput().id());
                        continue;
                    }
                    try {
                        lock.output = service.mechanism().extend(lock.input);
                    } catch (LockingConflictException e) {
                        service.activeLocks.remove(lock.lockInput().id());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to expire or extend lock {}: ", lock.input, e);
                }
            }
        }
    }

    /**
     * Attempts to clear all actively held locks from this {@link me.philcali.device.pool.lock.LockingService} instance.
     */
    public void clearLocks() {
        for (Map.Entry<String, Lock> stringLockEntry : activeLocks.entrySet()) {
            Lock lock = stringLockEntry.getValue();
            release(lock);
        }
    }

    /**
     * Attempts to release a single {@link me.philcali.device.pool.lock.LockingServiceModel.Lock} held by this {@link me.philcali.device.pool.lock.LockingService}.
     *
     * @param lock A {@link me.philcali.device.pool.lock.LockingServiceModel.Lock} instance held by this {@link me.philcali.device.pool.lock.LockingService}
     */
    public void release(Lock lock) {
        if (activeLocks.remove(lock.lockInput().id()) != null) {
            mechanism().lease(lock.lockInput().id());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (manageActiveLocks()) {
            Optional.ofNullable(extensionSchedule).ifPresent(future -> future.cancel(true));
            clearLocks();
        }
        activeLocks.clear();
    }
}
