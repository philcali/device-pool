/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.BaseDevicePool;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
    private Set<Host> seededHosts;

    @BeforeEach
    void setUp() {
        seededHosts = new HashSet<>();
        IntStream.range(0, 20).forEach(index -> seededHosts.add(Host.builder()
                .hostName("host-" + index)
                .deviceId("instance-123-" + index)
                .platform(PlatformOS.of("Linux", "armv8"))
                .build()));

        service = LocalProvisionService.builder()
                .addAllHosts(seededHosts)
                .provisionTimeout(TimeUnit.SECONDS.toMillis(5))
                .build();

        devicePool = BaseDevicePool.builder()
                .connections(connections)
                .transfers(transfers)
                .provisionAndReservationService(service)
                .build();
    }

    @Test
    void GIVEN_local_service_is_delegating_WHEN_manipulation_happens_THEN_change_events_propagate() {
        Set<Host> remaining = new HashSet<>(seededHosts);
        Set<Host> halfHosts = seededHosts.stream().limit(10).collect(Collectors.toSet());
        remaining.removeAll(halfHosts);
        LocalHostProvider hostProvider = new LocalHostProvider(halfHosts);
        service = LocalProvisionService.builder()
                .addAllHosts(seededHosts)
                .hostProvider(hostProvider)
                .provisionTimeout(TimeUnit.SECONDS.toMillis(5))
                .build();
        assertEquals(seededHosts, service.hostProvider().hosts());
        hostProvider.addHost(halfHosts.stream().findFirst().get());
        assertEquals(halfHosts, hostProvider.hosts());
        hostProvider.addHost(remaining.stream().findFirst().get());
        service.hostProvider().requestGrowth();
        hostProvider.removeListener((change, host) -> {

        });
    }

    @Test
    void GIVEN_local_service_WHEN_provisioning_empty_THEN_exception_is_thrown() {
        assertThrows(IllegalStateException.class, () -> LocalProvisionService.builder().build());
    }

    @Test
    void GIVEN_local_service_is_created_WHEN_delegating_providers_THEN_provisioning_is_synced() {
        LocalHostProvider hostProvider = new LocalHostProvider(seededHosts);
        LocalProvisionService newService = LocalProvisionService.builder()
                .from(service)
                .hosts(seededHosts)
                .hostProvider(hostProvider)
                .build();
        AtomicInteger added = new AtomicInteger();
        AtomicInteger removed = new AtomicInteger();
        newService.hostProvider().addListener((change, host) -> {
            if (change == HostProvider.HostChange.Add) {
                added.incrementAndGet();
            } else if (change == HostProvider.HostChange.Remove) {
                removed.incrementAndGet();
            }
        });
        hostProvider.removeHost(Host.builder()
                .hostName("host-0")
                .deviceId("instance-123-0")
                .platform(PlatformOS.of("Linux", "armv8"))
                .build());
        hostProvider.removeHost(Host.builder()
                .hostName("host-0")
                .deviceId("instance-123-0")
                .platform(PlatformOS.of("Linux", "armv8"))
                .build());
        hostProvider.addHost(Host.builder()
                .hostName("host-99")
                .deviceId("instance-123-99")
                .platform(PlatformOS.of("Linux", "armv8"))
                .build());
        hostProvider.addHost(Host.builder()
                .hostName("host-99")
                .deviceId("instance-123-99")
                .platform(PlatformOS.of("Linux", "armv8"))
                .build());
        assertEquals(hostProvider.hosts(), newService.hostProvider().hosts());
        assertEquals(1, added.get());
        assertEquals(1, removed.get());
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
        List<Device> devices = devicePool.provisionSync(input, 10, TimeUnit.SECONDS);
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

        assertThrows(ProvisioningException.class, () -> devicePool.provisionSync(anotherSet, 100, TimeUnit.MILLISECONDS));
        assertThrows(ProvisioningException.class, () -> devicePool.provisionSync(thirdSet, 100, TimeUnit.MILLISECONDS));
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
        List<Device> secondSet = devicePool.provisionSync(anotherSet, 10, TimeUnit.SECONDS);
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
                .addAllHosts(service.hostProvider().hosts())
                .expireProvisions(false)
                .executorService(newOne)
                .build();
        service.close();
        anotherOne.close();
        TimeUnit.SECONDS.sleep(1);
        newOne.shutdownNow();
    }

    @Test
    void GIVEN_no_local_service_WHEN_local_service_is_built_from_config_THEN_pool_is_configured() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader().getResourceAsStream("test/local.properties"));

        LocalProvisionService newService = LocalProvisionService.builder().fromConfig(config);

        Set<Host> expoectedHosts = Set.of(
                Host.builder()
                        .deviceId("host-0")
                        .hostName("127.0.0.1")
                        .port(8022)
                        .platform(PlatformOS.of("unix", "amd64"))
                        .build(),
                Host.builder()
                        .deviceId("host2")
                        .hostName("192.168.1.202")
                        .platform(PlatformOS.of("windows", "armv8"))
                        .build()
        );

        assertTrue(newService.expireProvisions());
        assertEquals(expoectedHosts, newService.hostProvider().hosts());
    }

    @Test
    void GIVEN_no_local_service_WHEN_local_service_is_built_from_defaults_THEN_pool_is_configured() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader().getResourceAsStream("test/default.local.properties"));

        LocalProvisionService newService = LocalProvisionService.builder().fromConfig(config);

        assertTrue(newService.expireProvisions());
        assertEquals(TimeUnit.HOURS.toMillis(1), newService.provisionTimeout());
        assertEquals(1, newService.hostProvider().hosts().size());
    }

    @Test
    void GIVEN_no_local_service_WHEN_local_service_is_built_from_defaults_THEN_exception_is_thrown() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader().getResourceAsStream("test/default.local2.properties"));

        assertThrows(ProvisioningException.class, () -> LocalProvisionService.builder().fromConfig(config));
    }
}
