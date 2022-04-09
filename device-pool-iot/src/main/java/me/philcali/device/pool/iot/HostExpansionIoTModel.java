/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.iot;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.provision.ExpandingHostProvider;
import org.immutables.value.Value;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsInThingGroupRequest;
import software.amazon.awssdk.services.iot.model.ListThingsInThingGroupResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApiModel
@Value.Immutable
abstract class HostExpansionIoTModel implements ExpandingHostProvider.ExpansionFunction {
    abstract String thingGroup();

    @Value.Default
    boolean recursive() {
        return true;
    }

    @Value.Default
    IotClient iot() {
        return IotClient.create();
    }

    @Value.Default
    PlatformOS platform() {
        return PlatformOS.of("unknown", "unknown");
    }

    @Value.Default
    Function<String, Host> thingToHost() {
        return thingName -> Host.builder()
                .deviceId(thingName)
                .hostName(thingName)
                .platform(platform())
                .build();
    }

    @Override
    public ExpandingHostProvider.NextSetHosts apply(String nextToken, Integer leases) {
        final ListThingsInThingGroupResponse response = iot().listThingsInThingGroup(
                ListThingsInThingGroupRequest.builder()
                        .thingGroupName(thingGroup())
                        .recursive(recursive())
                        .maxResults(leases)
                        .nextToken(nextToken)
                        .build());
        return new ExpandingHostProvider.NextSetHosts() {
            @Override
            public List<Host> hosts() {
                return response.things().stream()
                        .map(thingToHost())
                        .collect(Collectors.toList());
            }

            @Override
            public String nextToken() {
                return response.nextToken();
            }
        };
    }
}
