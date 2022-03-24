/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ProvisionService;
import me.philcali.device.pool.reservation.ReservationService;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.InstanceInformationStringFilter;
import software.amazon.awssdk.services.ssm.model.PingStatus;
import software.amazon.awssdk.services.ssm.paginators.DescribeInstanceInformationIterable;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@APIShadowModel
@Value.Immutable
public abstract class ProvisionServiceSSM implements ProvisionService, ReservationService {
    private final Map<String, ProvisionOutput> provisionalCache = new ConcurrentHashMap<>();

    @Value.Default
    SsmClient ssm() {
        return SsmClient.create();
    }

    @Value.Default
    ScheduledExecutorService executorService() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("SSM-Provisioning");
            return thread;
        });
    }

    @Value.Default
    ProvisionStrategy provisionStrategy() {
        return new FirstAvailableProvisionStrategy();
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

    abstract String architecture();

    public static final class Builder
            extends ImmutableProvisionServiceSSM.Builder
            implements ConfigBuilder<ProvisionServiceSSM> {
        @Override
        public ProvisionServiceSSM fromConfig(DevicePoolConfig config) {
            return config.namespace("provision.ssm")
                    .flatMap(entry -> {
                        entry.get("poolId").ifPresent(this::poolId);
                        return entry.get("architecture")
                                .map(this::architecture)
                                .map(ImmutableProvisionServiceSSM.Builder::build);
                    })
                    .orElseThrow(() -> new ProvisioningException("Could not construct an SSM provision service"));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Value.Default
    BiFunction<InstanceInformation, Host.Builder, Host.Builder> host() {
        return (information, builder) -> builder.port(22);
    }
    
    interface ProvisionStrategy {
        Set<InstanceInformation> instances(int requestedAmount, Stream<InstanceInformation> iterable);
    }
    
    public static final class FirstAvailableProvisionStrategy implements ProvisionStrategy {
        @Override
        public Set<InstanceInformation> instances(int requestedAmount, Stream<InstanceInformation> iterable) {
            return iterable.limit(requestedAmount).collect(Collectors.toSet());
        }
    }

    public static final class RandomizedProvisionStrategy implements ProvisionStrategy {
        public static final int DEFAULT_SIZE = 100;
        private final int poolSize;
        private final SecureRandom random;

        public RandomizedProvisionStrategy(final int poolSize) {
            this.poolSize = poolSize;
            this.random = new SecureRandom();
        }

        public RandomizedProvisionStrategy() {
            this(DEFAULT_SIZE);
        }

        @Override
        public Set<InstanceInformation> instances(int requestedAmount, Stream<InstanceInformation> iterable) {
            List<InstanceInformation> informationList = iterable.limit(poolSize).collect(Collectors.toList());
            final Set<InstanceInformation> rval = new HashSet<>();
            if (informationList.size() < requestedAmount) {
                rval.addAll(informationList);
            } else {
                while (rval.size() < requestedAmount) {
                    rval.add(informationList.get(random.nextInt(Math.min(poolSize, informationList.size()))));
                }
            }
            return rval;
        }
    }
    
    private class ProvisionalDriver implements Runnable {
        private final ProvisionInput input;
        
        ProvisionalDriver(ProvisionInput input) {
            this.input = input;
        }
        
        @Override
        public void run() {
            provisionalCache.computeIfPresent(input.id(), (id, output) -> {
                final Set<InstanceInformation> information = provisionStrategy().instances(input.amount(), ssm()
                        .describeInstanceInformationPaginator(builder -> builder.filters(filters()))
                        .instanceInformationList().stream()
                        .filter(instance -> instance.pingStatus() == PingStatus.ONLINE));
                return ProvisionOutput.builder().from(output)
                        .addAllReservations(information.stream()
                                .map(instance -> Reservation.of(instance.instanceId(), Status.SUCCEEDED))
                                .collect(Collectors.toList()))
                        .status(information.size() == input.amount() ? Status.SUCCEEDED : Status.FAILED)
                        .build();
            });
        }
    }

    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        return provisionalCache.computeIfAbsent(input.id(), id -> {
            final ProvisionOutput output = ProvisionOutput.builder()
                    .id(id)
                    .status(Status.PROVISIONING)
                    .build();
            executorService().execute(new ProvisionalDriver(input));
            return output;
        });
    }

    @Override
    public ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException {
        return Optional.ofNullable(provisionalCache.get(output.id()))
                .orElseThrow(() -> new ProvisioningException("Could not find a provision request " + output.id()));
    }

    public void flushTerminalProvisions() {
        for (Map.Entry<String, ProvisionOutput> entry : provisionalCache.entrySet()) {
            if (entry.getValue().status().isTerminal()) {
                provisionalCache.remove(entry.getKey());
            }
        }
    }

    @Override
    public Host exchange(Reservation reservation) throws ReservationException {
        DescribeInstanceInformationResponse response = ssm().describeInstanceInformation(
                DescribeInstanceInformationRequest.builder()
                        .filters(InstanceInformationStringFilter.builder()
                                .key("InstanceIds")
                                .values(reservation.deviceId())
                                .build())
                        .build());
        InstanceInformation information = response.instanceInformationList().stream()
                .findFirst()
                .orElseThrow(() -> new ReservationException("Could not find device: " + reservation.deviceId()));
        return host().apply(information, Host.builder()
                .platform(PlatformOS.of(information.platformType().toString(), architecture()))
                .hostName(information.ipAddress())
                .deviceId(information.instanceId()))
                .build();
    }

    @Override
    public void close() {
        ssm().close();
        provisionalCache.clear();
    }
}
