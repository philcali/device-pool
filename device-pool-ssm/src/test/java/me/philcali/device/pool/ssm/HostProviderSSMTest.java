/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.provision.HostProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.PingStatus;
import software.amazon.awssdk.services.ssm.model.PlatformType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class HostProviderSSMTest {
    @Mock
    private SsmClient ssm;

    private HostProviderSSM hostProvider;

    @BeforeEach
    void setUp() {
        hostProvider = HostProviderSSM.builder()
                .poolId("MyTest")
                .ssm(ssm)
                .architecture("armv7")
                .build();
    }

    @Test
    void GIVEN_host_provider_is_created_WHEN_requesting_growth_THEN_ssm_is_queried()
            throws ExecutionException, InterruptedException, TimeoutException {
        DescribeInstanceInformationResponse response = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("aaa-111").ipAddress("10.0.1.1").platformType(PlatformType.LINUX),
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("bbb-111").ipAddress("10.0.1.2").platformType(PlatformType.WINDOWS),
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("ccc-111").ipAddress("10.0.1.3").platformType(PlatformType.LINUX)
                )
                .build();
        DescribeInstanceInformationRequest initialRequest = DescribeInstanceInformationRequest.builder()
                .filters(hostProvider.filters())
                .build();
        doReturn(response).when(ssm).describeInstanceInformation(eq(initialRequest));
        AtomicInteger count = new AtomicInteger();
        hostProvider.addListener((change, host) -> {
            if (change == HostProvider.HostChange.Add) {
                count.incrementAndGet();
            }
        });
        hostProvider.requestGrowth();
        assertTrue(CompletableFuture.supplyAsync(() -> {
            for (;;) {
                if (count.get() >= response.instanceInformationList().size()) {
                    return true;
                }
            }
        }).get(5, TimeUnit.SECONDS));
    }

    @Test
    void GIVEN_host_provider_is_created_WHEN_requesting_paging_growth_THEN_SSM_is_paged() throws ExecutionException, InterruptedException, TimeoutException {
        hostProvider = HostProviderSSM.builder()
                .ssm(ssm)
                .architecture("armv7")
                .build();
        DescribeInstanceInformationResponse firstResponse = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("aaa-111").ipAddress("10.0.1.1").platformType(PlatformType.LINUX),
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("bbb-111").ipAddress("10.0.1.2").platformType(PlatformType.WINDOWS),
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("ccc-111").ipAddress("10.0.1.3").platformType(PlatformType.LINUX)
                )
                .build();
        DescribeInstanceInformationResponse secondResponse = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("aaa-111").ipAddress("10.0.1.1").platformType(PlatformType.LINUX),
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("ccc-111").ipAddress("10.0.1.3").platformType(PlatformType.LINUX)
                )
                .nextToken("abc-123")
                .build();
        DescribeInstanceInformationResponse thirdResponse = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("ddd-111").ipAddress("10.0.1.4").platformType(PlatformType.LINUX),
                        instance -> instance.pingStatus(PingStatus.ONLINE).instanceId("fff-111").ipAddress("10.0.1.5").platformType(PlatformType.LINUX)
                )
                .build();
        DescribeInstanceInformationRequest initialRequest = DescribeInstanceInformationRequest.builder()
                .filters(hostProvider.filters())
                .build();
        AtomicInteger callCount = new AtomicInteger();
        when(ssm.describeInstanceInformation(eq(initialRequest))).then(answer -> {
            if (callCount.getAndIncrement() == 0) {
                return firstResponse;
            } else {
                return secondResponse;
            }
        });
        doReturn(thirdResponse).when(ssm).describeInstanceInformation(eq(DescribeInstanceInformationRequest.builder()
                .filters(hostProvider.filters())
                .nextToken("abc-123")
                .build()));
        final Set<Host> reactiveSet = new HashSet<>();
        hostProvider.addListener((change, host) -> {
            if (change == HostProvider.HostChange.Add) {
                reactiveSet.add(host);
            } else {
                reactiveSet.remove(host);
            }
        });
        hostProvider.requestGrowth();
        assertTrue(CompletableFuture.supplyAsync(() -> {
            for (;;) {
                if (reactiveSet.size() == firstResponse.instanceInformationList().size()) {
                    return true;
                }
            }
        }).get(5, TimeUnit.SECONDS));
        assertEquals(hostProvider.hosts(), reactiveSet);
        hostProvider.requestGrowth();
        assertTrue(CompletableFuture.supplyAsync(() -> {
            for (;;) {
                if (!reactiveSet.contains(Host.builder().platform(PlatformOS.of("Windows", hostProvider.architecture())).hostName("10.0.1.2").deviceId("bbb-111").port(22).build())) {
                    return true;
                }
            }
        }).get(5, TimeUnit.SECONDS));
        assertEquals(hostProvider.hosts(), reactiveSet);
        Set<Host> expected = new HashSet<>(Arrays.asList(
                Host.builder().platform(PlatformOS.of("Linux", hostProvider.architecture())).hostName("10.0.1.1").deviceId("aaa-111").port(22).build(),
                Host.builder().platform(PlatformOS.of("Linux", hostProvider.architecture())).hostName("10.0.1.3").deviceId("ccc-111").port(22).build(),
                Host.builder().platform(PlatformOS.of("Linux", hostProvider.architecture())).hostName("10.0.1.4").deviceId("ddd-111").port(22).build(),
                Host.builder().platform(PlatformOS.of("Linux", hostProvider.architecture())).hostName("10.0.1.5").deviceId("fff-111").port(22).build()
        ));
        assertEquals(expected, hostProvider.hosts());
    }

    @Test
    void GIVEN_host_provider_WHEN_closing_THEN_ssm_is_closed() {
        hostProvider.close();
        verify(ssm).close();
    }
}