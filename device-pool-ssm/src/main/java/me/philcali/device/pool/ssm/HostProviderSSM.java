/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.provision.ExpandingHostProvider;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.InstanceInformationStringFilter;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

@APIShadowModel
@Value.Immutable
@Deprecated
public abstract class HostProviderSSM extends ExpandingHostProvider {
    private static final int DEFAULT_CACHE_THRESHOLD = 100;

    abstract SsmClient ssm();

    @Value.Default
    @Deprecated
    int cacheThreshold() {
        return DEFAULT_CACHE_THRESHOLD;
    }

    @Override
    public int maximumIncrementalLeases() {
        return DEFAULT_CACHE_THRESHOLD;
    }

    public static final class Builder extends ImmutableHostProviderSSM.Builder {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ExpansionFunction expansionFunction() {
        return HostExpansionSSM.builder()
                .architecture(architecture())
                .poolId(poolId())
                .ssm(ssm())
                .filters(filters())
                .hostBuilder(hostBuilder())
                .build();
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
    public void close() {
        super.close();
        ssm().close();
    }
}
