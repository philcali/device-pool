/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.provision.AbstractHostProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.InstanceInformationStringFilter;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

@ApiModel
@Value.Immutable
abstract class HostProviderSSMModel extends AbstractHostProvider implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(HostProviderSSM.class);
    private static final int DEFAULT_CACHE_THRESHOLD = 100;
    private final Set<Host> hosts = new HashSet<>();
    private final AtomicReference<InstanceInformation> lastReadInstance = new AtomicReference<>();
    private final AtomicReference<Integer> pageNumber = new AtomicReference<>();
    private final Lock expansionLock = new ReentrantLock();

    abstract SsmClient ssm();

    @Value.Default
    int cacheThreshold() {
        return DEFAULT_CACHE_THRESHOLD;
    }

    @Value.Default
    ExecutorService executorService() {
        return Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("SSM-HostCache");
            return thread;
        });
    }

    abstract String architecture();

    @Value.Default
    BiFunction<InstanceInformation, Host.Builder, Host.Builder> hostBuilder() {
        return (instance, builder) -> builder
                .port(22)
                .hostName(instance.ipAddress())
                .deviceId(instance.instanceId())
                .platform(PlatformOS.of(instance.platformType().toString(), architecture()));
    }

    @Nullable
    abstract String poolId();

    @Value.Default
    Collection<InstanceInformationStringFilter> filters() {
        final List<InstanceInformationStringFilter> filters = new ArrayList<>();
        if (poolId() != null) {
            filters.add(InstanceInformationStringFilter.builder()
                    .key("tag:DevicePool")
                    .values(poolId())
                    .build());
        } else {
            filters.add(InstanceInformationStringFilter.builder()
                    .key("PingStatus")
                    .values(PingStatus.ONLINE.toString())
                    .build());
        }
        return Collections.unmodifiableList(filters);
    }

    @Override
    public Set<Host> hosts() {
        return Collections.unmodifiableSet(hosts);
    }

    private class SSMHostExpansionRunnable implements Runnable {
        @Override
        public void run() {
            expansionLock.lock();
            try {
                Comparator<InstanceInformation> byInstanceId = Comparator.comparing(InstanceInformation::instanceId);
                boolean needsExpansion;
                final Set<Host> current = new HashSet<>();
                String nextToken = null;
                int currentPage = 0;
                do {
                    DescribeInstanceInformationResponse response = ssm()
                            .describeInstanceInformation(DescribeInstanceInformationRequest.builder()
                                    .filters(filters())
                                    .nextToken(nextToken)
                                    .build());
                    nextToken = response.nextToken();
                    response
                            .instanceInformationList()
                            .stream()
                            .filter(instance -> instance.pingStatus().equals(PingStatus.ONLINE))
                            .map(instance -> hostBuilder().apply(instance, Host.builder()).build())
                            .forEach(current::add);
                    boolean thereIsMore = false;
                    if (!response.instanceInformationList().isEmpty()) {
                        InstanceInformation lastRead = response.instanceInformationList()
                                .get(response.instanceInformationList().size() - 1);
                        lastReadInstance.accumulateAndGet(lastRead,
                                (left, right) -> left == null || byInstanceId.compare(left, right) <= 0 ? right : left);
                        // Meaning, the last read instance in this page is less than the one we read previously.
                        thereIsMore = byInstanceId.compare(lastRead, lastReadInstance.get()) <= 0;
                    }
                    needsExpansion = Objects.nonNull(nextToken)
                            && thereIsMore
                            && currentPage <= pageNumber.get();
                    currentPage++;
                } while (needsExpansion);
                pageNumber.set(currentPage);
                // Attempt to add any new ones found
                current.forEach(host -> {
                    if (hosts.add(host)) {
                        trigger(HostChange.Add, host);
                    }
                });
                // Any left over, means it's no longer in the pool
                Iterator<Host> hostIterator = hosts.iterator();
                while (hostIterator.hasNext()) {
                    Host removed = hostIterator.next();
                    if (!current.contains(removed)) {
                        hostIterator.remove();
                        trigger(HostChange.Remove, removed);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to expand SSM host population {}", poolId(), e);
            } finally {
                expansionLock.unlock();
            }
        }
    }

    @Override
    public void requestGrowth() {
        super.requestGrowth();
        executorService().execute(new SSMHostExpansionRunnable());
    }

    @Override
    public void close() {
        executorService().shutdownNow();
        ssm().close();
    }
}
