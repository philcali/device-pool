/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

@APIShadowModel
@Value.Immutable
public abstract class ExpandingHostProvider extends AbstractHostProvider implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(ExpandingHostProvider.class);
    private static final int DEFAULT_LEASE_SIZE = 20;
    private final Set<Host> hosts = new HashSet<>();
    private final Lock expansionLock = new ReentrantLock();

    @Value.Default
    ExecutorService executorService() {
        return Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setName("Host-Expansion-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static final class Builder
            extends ImmutableExpandingHostProvider.Builder {
    }

    @Value.Default
    public int maximumIncrementalLeases() {
        return DEFAULT_LEASE_SIZE;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static ExpandingHostProvider of(ExpansionFunction function) {
        return newBuilder().expansionFunction(function).build();
    }

    public abstract ExpansionFunction expansionFunction();

    @Override
    public Set<Host> hosts() {
        return Collections.unmodifiableSet(hosts);
    }

    @Override
    public void requestGrowth() {
        super.requestGrowth();
        executorService().execute(new ExpansionRunnable());
    }

    @FunctionalInterface
    public interface ExpansionFunction extends BiFunction<String, Integer, NextSetHosts> {
    }

    public interface NextSetHosts {
        List<Host> hosts();

        String nextToken();
    }

    class ExpansionRunnable implements Runnable {
        @Override
        public void run() {
            LOGGER.info("Expanding hosts, current size: {}", hosts.size());
            expansionLock.lock();
            try {
                int leases = maximumIncrementalLeases();
                Set<Host> current = new HashSet<>(hosts);
                String nextToken = null;
                do {
                    final NextSetHosts nextPage = expansionFunction()
                            .apply(nextToken, maximumIncrementalLeases());
                    for (Host host : nextPage.hosts()) {
                        if (hosts.add(host)) {
                            trigger(HostChange.Add, host);
                            leases--;
                        } else {
                            current.remove(host);
                        }
                    }
                    nextToken = nextPage.nextToken();
                    // It's OK that we're "best effort" in leasing.
                } while (leases > 0 && Objects.nonNull(nextToken));
                for (Host host : current) {
                    if (hosts.remove(host)) {
                        trigger(HostChange.Remove, host);
                    }
                }
                LOGGER.info("Expanded hosts, new size {}", hosts.size());
            } catch (Exception e) {
                LOGGER.error("Failure to expand hosts: ", e);
            } finally {
                expansionLock.unlock();
            }
        }
    }

    @Override
    public void close() {
        executorService().shutdownNow();
    }
}
