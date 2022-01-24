/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.lock.LockingMechanism;
import me.philcali.device.pool.lock.LockingService;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class LockingProvisionServiceTest {
    @Mock
    private ProvisionService provisionService;
    @Mock
    private LockingMechanism lockingMechanism;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    private LockingService lockingService;
    private ProvisionService lockingProvisionService;

    @BeforeEach
    void setup() {
        lockingService = LockingService.builder()
                .manageActiveLocks(false)
                .mechanism(lockingMechanism)
                .executorService(scheduledExecutorService)
                .build();
        lockingProvisionService = LockingProvisionService.builder()
                .lockingFunction(input -> LockInput.of(input.id() + "-test"))
                .lockingService(lockingService)
                .provisionService(provisionService)
                .build();
    }

    @AfterEach
    void teardown() {
        lockingService.close();
    }

    @Test
    void GIVEN_locking_provision_service_WHEN_provision_is_invoked_THEN_blocks_on_lock() {
        ProvisionInput input = ProvisionInput.builder()
                .id("abc-123")
                .amount(5)
                .build();
        ProvisionOutput provisionOutput = ProvisionOutput.of(input.id());
        LockOutput output = LockOutput.of("abc-123-test",
                Instant.now().getEpochSecond(),
                Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
        when(lockingMechanism.lock(eq(LockInput.of("abc-123-test")))).thenReturn(CompletableFuture.completedFuture(output));
        when(provisionService.provision(eq(input))).thenReturn(provisionOutput);
        assertEquals(provisionOutput, lockingProvisionService.provision(input));
    }

    @Test
    void GIVEN_locking_provision_service_WHEN_provision_is_interrupted_THEN_exception_is_thrown()
            throws ExecutionException, InterruptedException {
        ProvisionInput input = ProvisionInput.builder()
                .id("abc-123")
                .amount(5)
                .build();
        CompletableFuture<LockOutput> future = spy(new CompletableFuture<>());
        doThrow(InterruptedException.class).when(future).get();
        when(lockingMechanism.lock(eq(LockInput.of("abc-123-test")))).thenReturn(future);
        assertThrows(ProvisioningException.class, () -> lockingProvisionService.provision(input));
    }

    @Test
    void GIVEN_locking_provision_service_WHEN_describe_is_invoked_THEN_forwards_to_service() {
        ProvisionOutput provisionOutput = ProvisionOutput.of("abc-123");
        lockingProvisionService.describe(provisionOutput);
        verify(provisionService).describe(eq(provisionOutput));
    }
}
