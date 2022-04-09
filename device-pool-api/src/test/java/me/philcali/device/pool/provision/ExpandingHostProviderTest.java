/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpandingHostProviderTest {
    private ExpandingHostProvider hostProvider;
    private ExpandingHostProvider.ExpansionFunction function;

    @BeforeEach
    void setUp() {
        function = new ExpandingHostProvider.ExpansionFunction() {
            ThreadLocal<Integer> timesCalled;

            @Override
            public ExpandingHostProvider.NextSetHosts apply(String nextToken, Integer leases) {
                if (timesCalled == null) {
                    timesCalled = ThreadLocal.withInitial(() -> 0);
                }
                final List<Host> hosts = new ArrayList<>();
                int minBinding = 0;
                final String pagingToken;
                if (nextToken == null) {
                    pagingToken = "aaa-111";
                    minBinding += timesCalled.get();
                } else if (nextToken.equals("aaa-111")) {
                    pagingToken = "bbb-222";
                    minBinding += leases;
                } else {
                    pagingToken = null;
                    minBinding += (leases * 2);
                }
                int maxBinding = minBinding + leases - timesCalled.get();
                timesCalled.set(timesCalled.get() + 1);
                for (int i = minBinding; i < maxBinding; i++) {
                    hosts.add(Host.builder()
                            .deviceId("host-" + i)
                            .hostName("host-" + i + ".com")
                            .platform(PlatformOS.of("unknown", "unknown"))
                            .build());
                }
                return new ExpandingHostProvider.NextSetHosts() {
                    @Override
                    public List<Host> hosts() {
                        return hosts;
                    }

                    @Override
                    public String nextToken() {
                        return pagingToken;
                    }
                };
            }
        };

        hostProvider = ExpandingHostProvider.of(function);
    }

    @Test
    void GIVEN_expanding_host_provider_WHEN_expanding_hosts_THEN_hosts_expand_serially()
            throws Exception {
        final Set<Host> spyHosts = Collections.synchronizedSet(new HashSet<>());
        hostProvider.addListener((change, host) -> {
            if (change == HostProvider.HostChange.Add) {
                spyHosts.add(host);
            } else {
                spyHosts.remove(host);
            }
        });
        assertEquals(0, spyHosts.size());
        assertEquals(spyHosts, hostProvider.hosts());
        // First set
        hostProvider.requestGrowth();
        ExpandingHostProvider.NextSetHosts nextSet = function
                .apply(null, hostProvider.maximumIncrementalLeases());
        CompletableFuture<Boolean> firstPage = CompletableFuture.supplyAsync(() -> {
            for (;;) {
                if (spyHosts.containsAll(nextSet.hosts())) {
                    return true;
                }
            }
        });
        assertEquals("aaa-111", nextSet.nextToken());
        assertTrue(firstPage.get(1, TimeUnit.SECONDS));
        assertEquals(spyHosts, hostProvider.hosts());
        // Second set
        Lock lock = new ReentrantLock();
        Condition expansionComplete = lock.newCondition();
        hostProvider.requestGrowth();
        ExpandingHostProvider.NextSetHosts secondSet = function
                .apply(nextSet.nextToken(), hostProvider.maximumIncrementalLeases());
        ExpandingHostProvider.NextSetHosts thirdSet = function
                .apply(secondSet.nextToken(), hostProvider.maximumIncrementalLeases());
        assertEquals("bbb-222", secondSet.nextToken());
        assertNull(thirdSet.nextToken());
        // seed the completion condition on the single threaded execution queue
        hostProvider.executorService().execute(() -> {
            lock.lock();
            try {
                expansionComplete.signal();
            } finally {
                lock.unlock();
            }
        });
        lock.lock();
        assertTrue(expansionComplete.await(1, TimeUnit.SECONDS));
        lock.unlock();
        assertEquals(spyHosts, hostProvider.hosts());
        // max lease * 3 - (1 - 2 - 3) == 54
        assertEquals(54, spyHosts.size());

        hostProvider.close();
    }
}
