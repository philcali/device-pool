/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.InstanceInformationStringFilter;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

interface HostProvisionMixin {
    @Nullable
    String poolId();

    String architecture();

    @Value.Default
    default BiFunction<InstanceInformation, Host.Builder, Host.Builder> hostBuilder() {
        return (instance, builder) -> builder
                .port(22)
                .hostName(instance.ipAddress())
                .deviceId(instance.instanceId())
                .platform(PlatformOS.of(instance.platformType().toString(), architecture()));
    }

    @Value.Default
    default Collection<InstanceInformationStringFilter> filters() {
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
}
