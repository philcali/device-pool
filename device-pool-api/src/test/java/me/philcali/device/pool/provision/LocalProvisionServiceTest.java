/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class LocalProvisionServiceTest {
    private LocalProvisionService service;
    private DevicePool devicePool;

    @Mock
    private ConnectionFactory connections;
    @Mock
    private ContentTransferAgentFactory transfers;
    @Mock
    private Connection connection;
    @Mock
    private ContentTransferAgent agent;

    @BeforeEach
    void setUp() {
        List<Host> hosts = new ArrayList<>();
        IntStream.range(0, 20).forEach(index -> hosts.add(Host.builder()
                .hostName("host-" + index)
                .deviceId("instance-123-" + index)
                .platform(PlatformOS.of("Linux", "armv8"))
                .build()));

        service = LocalProvisionService.builder()
                .addAllHosts(hosts)
                .provisionTimeout(TimeUnit.SECONDS.toMillis(5))
                .build();

        devicePool = BaseDevicePool.builder()
                .connections(connections)
                .transfers(transfers)
                .provisionAndReservationService(service)
                .build();
    }

    @Test
    void GIVEN_local_service_is_created_WHEN_provisioning_THEN_obtains_devices() throws Exception {
        // The point of this test is to not exercise data-plane
        doReturn(connection).when(connections).connect(any(Host.class));
        doReturn(agent).when(transfers).connect(anyString(), eq(connection), any(Host.class));

        ProvisionInput input = ProvisionInput.builder()
                .id("first-test")
                .amount(20)
                .build();
        List<Device> devices = devicePool.provisionWait(input, 10, TimeUnit.SECONDS);
        assertEquals(20, devices.size());

        ProvisionInput anotherSet = ProvisionInput.builder()
                .id("second-test")
                .amount(3)
                .build();
        ProvisionInput thirdSet = ProvisionInput.builder()
                .id("third-test")
                .amount(3)
                .build();
        service.extend(ProvisionOutput.of(input.id()));

        assertThrows(ProvisioningException.class, () -> devicePool.provisionWait(anotherSet, 100, TimeUnit.MILLISECONDS));
        assertThrows(ProvisioningException.class, () -> devicePool.provisionWait(thirdSet, 100, TimeUnit.MILLISECONDS));
        // Force removal
        service.release(ProvisionOutput.of(anotherSet.id()));
        // Skips this one
        service.release(ProvisionOutput.of(thirdSet.id()));
        // Exercise break check, and flush to valid entry
        service.release(devices.get(0));

        final long waitTime = TimeUnit.MILLISECONDS.toMillis(500);
        Thread timebomb = new Thread(() -> {
            try {
                Thread.sleep(waitTime);
                devices.stream().skip(1).limit(2).forEach(service::release);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long startTime = System.currentTimeMillis();
        timebomb.start();
        List<Device> secondSet = devicePool.provisionWait(anotherSet, 10, TimeUnit.SECONDS);
        // Timebomb enacted, provisioning unlocked
        assertTrue(System.currentTimeMillis() - startTime >= waitTime);
        assertTrue(devices.containsAll(secondSet));

        assertEquals(3, service.releaseAvailable(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5)));
        assertEquals(0, service.releaseAvailable(System.currentTimeMillis()));

        service.close();
    }

    @Test
    void GIVEN_local_service_created_WHEN_extraneous_paths_are_exercised_THEN_coverage_is_increased() throws Exception {
        service.close();
        assertThrows(ProvisioningException.class, () -> service.describe(ProvisionOutput.of("non-existent")));
        assertEquals(0, service.release(ProvisionOutput.of("non-existence")));

        assertThrows(IllegalArgumentException.class, () -> LocalProvisionService.builder()
                .addHosts()
                .build());

        ExecutorService newOne = Executors.newCachedThreadPool();
        LocalProvisionService anotherOne = LocalProvisionService.builder()
                .addAllHosts(service.hosts())
                .expireProvisions(false)
                .executorService(newOne)
                .build();
        newOne.shutdownNow();
        anotherOne.close();
    }
}
