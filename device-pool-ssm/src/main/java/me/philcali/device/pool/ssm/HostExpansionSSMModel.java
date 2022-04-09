/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.provision.ExpandingHostProvider;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import java.util.List;
import java.util.stream.Collectors;

@ApiModel
@Value.Immutable
abstract class HostExpansionSSMModel implements ExpandingHostProvider.ExpansionFunction, HostProvisionMixin {
    @Value.Default
    SsmClient ssm() {
        return SsmClient.create();
    }

    @Override
    public ExpandingHostProvider.NextSetHosts apply(String nextToken, Integer leases) {
        final DescribeInstanceInformationResponse response = ssm()
                .describeInstanceInformation(DescribeInstanceInformationRequest.builder()
                        .filters(filters())
                        .nextToken(nextToken)
                        .maxResults(leases)
                        .build());
        return new ExpandingHostProvider.NextSetHosts() {
            @Override
            public List<Host> hosts() {
                return response.instanceInformationList()
                        .stream()
                        .filter(instance -> instance.pingStatus().equals(PingStatus.ONLINE))
                        .map(instance -> hostBuilder().apply(instance, Host.builder()).build())
                        .collect(Collectors.toList());
            }

            @Override
            public String nextToken() {
                return response.nextToken();
            }
        };
    }
}
