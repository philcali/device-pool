package me.philcali.device.pool.lock;

import me.philcali.device.pool.exceptions.LockingConflictException;
import me.philcali.device.pool.exceptions.LockingException;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class LockingServiceTest {
    @Mock
    private LockingMechanism lockingMechanism;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private ScheduledFuture<Void> scheduledFuture;
    private LockingService lockingService;
    private final AtomicReference<Runnable> lockRunner = new AtomicReference<>();

    @BeforeEach
    void setup() {
        when(scheduledExecutorService.scheduleWithFixedDelay(any(), eq(0L), eq(1000L), eq(TimeUnit.MILLISECONDS)))
                .then(answer -> {
                    Runnable runnable = answer.getArgument(0);
                    lockRunner.set(runnable);
                    return scheduledFuture;
                });
        lockingService = LockingService.builder()
                .executorService(scheduledExecutorService)
                .mechanism(lockingMechanism)
                .extensionInterval(1000L)
                .build();
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_acquiring_THEN_blocks_until_can_acquire() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        LockOutput output = LockOutput.of("abc-123",
                Instant.now().getEpochSecond(),
                Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
        CompletableFuture<LockOutput> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return output;
        });
        when(lockingMechanism.lock(eq(LockInput.of("abc-123")))).thenReturn(future);
        try (LockingService.Lock lock = lockingService.tryAcquire(LockInput.of("abc-123"))) {
            assertFalse(lock.isExpired());
            assertEquals(LockInput.of("abc-123"), lock.lockInput());
            assertTrue(System.currentTimeMillis() - startTime >= 500);
        }
        verify(lockingMechanism).lease(eq("abc-123"));
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_lock_expires_THEN_runnable_will_clear() throws InterruptedException {
        LockInput input = LockInput.of("abc-123");
        LockOutput output = LockOutput.of("abc-123",
                Instant.now().getEpochSecond(),
                Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond());
        when(lockingMechanism.lock(eq(input))).thenReturn(CompletableFuture.completedFuture(output));

        LockingService.Lock lock = lockingService.tryAcquire(input);
        assertTrue(lock.isExpired());
        lockRunner.get().run();
        lockingService.clearLocks();
        verify(lockingMechanism, times(0)).lease(eq(input.id()));
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_lock_is_active_THEN_runnable_will_extend() throws InterruptedException {
        LockInput input = LockInput.of("abc-123");
        LockOutput output = LockOutput.of("abc-123",
                Instant.now().getEpochSecond(),
                Instant.now().plus(1, ChronoUnit.MINUTES).getEpochSecond());
        when(lockingMechanism.lock(eq(input))).thenReturn(CompletableFuture.completedFuture(output));

        when(lockingMechanism.extend(eq(input))).thenReturn(LockOutput.builder().from(output)
                .updatedAt(Instant.now().getEpochSecond())
                .expiresIn(Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond())
                .build());

        LockingService.Lock lock = lockingService.tryAcquire(input);
        assertFalse(lock.isExpired());
        lockRunner.get().run();
        verify(lockingMechanism).extend(eq(input));
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_lock_is_active_THEN_runnable_will_remove_on_conflict() throws InterruptedException {
        LockInput input = LockInput.of("abc-123");
        LockOutput output = LockOutput.of("abc-123",
                Instant.now().getEpochSecond(),
                Instant.now().plus(1, ChronoUnit.MINUTES).getEpochSecond());
        when(lockingMechanism.lock(eq(input))).thenReturn(CompletableFuture.completedFuture(output));
        when(lockingMechanism.extend(eq(input))).thenThrow(LockingConflictException.class);

        LockingService.Lock lock = lockingService.tryAcquire(input);
        assertFalse(lock.isExpired());
        lockRunner.get().run();
        lockingService.clearLocks();
        verify(lockingMechanism, times(0)).lease(eq(input.id()));
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_acquiring_times_out_THEN_exception_is_thrown() {
        CompletableFuture<LockOutput> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return LockOutput.of("abc-123",
                    Instant.now().getEpochSecond(),
                    Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
        });
        when(lockingMechanism.lock(eq(LockInput.of("abc-123")))).thenReturn(future);
        assertThrows(LockingException.class, () -> lockingService.tryAcquire(LockInput.of("abc-123"), 100L, TimeUnit.MILLISECONDS));
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_acquiring_fails_THEN_exception_is_thrown() {
        CompletableFuture<LockOutput> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("fail"));
        when(lockingMechanism.lock(eq(LockInput.of("abc-123")))).thenReturn(future);
        assertThrows(LockingException.class, () -> lockingService.tryAcquire(LockInput.of("abc-123"), 100L, TimeUnit.MILLISECONDS));
    }

    @Test
    void GIVEN_locking_service_is_created_WHEN_closing_THEN_cleans_everything_up() throws InterruptedException {
        assertNotNull(lockRunner.get());

        LockOutput output = LockOutput.of("abc-123",
                Instant.now().getEpochSecond(),
                Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
        when(lockingMechanism.lock(eq(LockInput.of("abc-123")))).thenReturn(CompletableFuture.completedFuture(output));

        lockingService.tryAcquire(LockInput.of("abc-123"));
        lockingService.close();
        verify(lockingMechanism).lease(eq("abc-123"));
        verify(scheduledFuture).cancel(eq(true));
    }
}
