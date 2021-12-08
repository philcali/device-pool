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

@ApiModel
@Value.Immutable
abstract class LockingServiceModel implements AutoCloseable {
    protected static final Logger LOGGER = LogManager.getLogger(LockingServiceModel.class);
    protected final Map<String, Lock> activeLocks = new ConcurrentHashMap<>();
    protected ScheduledFuture<Void> extensionSchedule;

    abstract boolean manageActiveLocks();

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

    public Lock tryAcquire(LockInput input, long amount, TimeUnit unit) throws InterruptedException {
        return lock(input, future -> future.get(amount, unit));
    }

    public Lock tryAcquire(LockInput input) throws InterruptedException {
        return lock(input, CompletableFuture::get);
    }

    public class Lock implements AutoCloseable {
        private final LockInput input;
        private LockOutput output;

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
                        service.activeLocks.remove(lock.lockInput());
                        continue;
                    }
                    try {
                        lock.output = service.mechanism().extend(lock.input);
                    } catch (LockingConflictException e) {
                        service.activeLocks.remove(lock.lockInput());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to expire or extend lock {}: ", lock.input, e);
                }
            }
        }
    }

    public void clearLocks() {
        for (Map.Entry<String, Lock> stringLockEntry : activeLocks.entrySet()) {
            Lock lock = stringLockEntry.getValue();
            release(lock);
        }
    }

    public void release(Lock lock) {
        if (activeLocks.remove(lock.lockInput()) != null) {
            mechanism().lease(lock.lockInput().id());
        }
    }

    @Override
    public void close() {
        if (manageActiveLocks()) {
            Optional.ofNullable(extensionSchedule).ifPresent(future -> future.cancel(true));
            clearLocks();
        }
        activeLocks.clear();
    }
}
