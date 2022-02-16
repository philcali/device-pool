/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.lock;

import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLockingMechanismTest {

    private LocalLockingMechanism lockingMechanism;

    @BeforeEach
    void setUp() {
        lockingMechanism = new LocalLockingMechanism();
    }

    @Test
    void GIVEN_local_mechanism_WHEN_locking_THEN_a_lock_is_maintained()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<LockOutput> tryLock = lockingMechanism.lock(LockInput.builder()
                .id("abc-123")
                .holder("test1")
                .build());

        LockOutput lock = tryLock.get();
        assertEquals("abc-123", lock.id());

        assertThrows(TimeoutException.class, () -> lockingMechanism.lock(LockInput.builder()
                .id("abc-123")
                .holder("test2")
                .build())
                .get(1, TimeUnit.SECONDS));

        LockOutput lock2 = lockingMechanism.extend(LockInput.builder()
                .id("abc-123")
                .holder("test1")
                .build());
        assertTrue(lock2.expiresIn() > lock.expiresIn());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> lockingMechanism.lease("abc-123"), 100, TimeUnit.MILLISECONDS);

        LockOutput replace = lockingMechanism.lock(LockInput.builder()
                        .id("abc-123")
                        .holder("test2")
                        .build())
                .get(1, TimeUnit.SECONDS);
        assertEquals("abc-123", replace.id());
    }
}
