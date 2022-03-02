/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.PingStatus;
import software.amazon.awssdk.services.ssm.model.PlatformType;
import software.amazon.awssdk.services.ssm.paginators.DescribeInstanceInformationIterable;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class ProvisionServiceSSMTest {
    @Mock
    SsmClient ssm;

    ProvisionServiceSSM provisionService;

    @BeforeEach
    void setUp() {
        provisionService = ProvisionServiceSSM.builder()
                .architecture("armv6")
                .poolId("picameras")
                .ssm(ssm)
                .build();
    }

    @Test
    void GIVEN_provision_service_WHEN_provisioning_THEN_ssm_instances_are_returned()
            throws ExecutionException, InterruptedException, TimeoutException {
        DescribeInstanceInformationRequest firstRequest = DescribeInstanceInformationRequest.builder()
                .filters(
                        filter -> filter.key("tag:DevicePool").values("picameras"),
                        filter -> filter.key("PingStatus").values(PingStatus.ONLINE.toString())
                )
                .build();
        DescribeInstanceInformationIterable iterable = new DescribeInstanceInformationIterable(ssm, firstRequest);
        DescribeInstanceInformationResponse response = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.instanceId("abc-123").ipAddress("127.0.0.1"),
                        instance -> instance.instanceId("efg-456").ipAddress("192.168.1.1")
                )
                .build();
        doReturn(iterable).when(ssm).describeInstanceInformationPaginator(any(Consumer.class));
        doReturn(response).when(ssm).describeInstanceInformation(eq(firstRequest));

        ProvisionInput input = ProvisionInput.create();
        provisionService.provision(input);

        // Operation is async ... force scheduled completion
        ProvisionOutput result = CompletableFuture.supplyAsync(() -> {
            ProvisionOutput output = provisionService.provision(input);
            while (!output.status().isTerminal()) {
                output = provisionService.describe(output);
            }
            return output;
        }).get(5, TimeUnit.SECONDS);

        ProvisionOutput expected = ProvisionOutput.builder()
                .id(input.id())
                .status(Status.SUCCEEDED)
                .addReservations(Reservation.of("abc-123", Status.SUCCEEDED))
                .build();
        assertEquals(expected, result);

        assertThrows(ProvisioningException.class, () -> provisionService.describe(ProvisionOutput.of("non-exist")));
        // called only once... cached provision
        IntStream.range(1, 5).forEach(time -> {
            assertEquals(expected, provisionService.provision(input));
        });
        verify(ssm).describeInstanceInformation(eq(firstRequest));

        provisionService.flushTerminalProvisions();
        assertThrows(ProvisioningException.class, () -> provisionService.describe(result));
    }

    @Test
    void GIVEN_provision_service_WHEN_provisioning_randomly_THEN_ssm_instances_are_returned()
            throws ExecutionException, InterruptedException, TimeoutException {
        provisionService = ProvisionServiceSSM.builder()
                .ssm(ssm)
                .architecture("armv6")
                .provisionStrategy(new ProvisionServiceSSM.RandomizedProvisionStrategy())
                .executorService(provisionService.executorService())
                .build();
        DescribeInstanceInformationRequest firstRequest = DescribeInstanceInformationRequest.builder()
                .filters(
                        filter -> filter.key("PingStatus").values(PingStatus.ONLINE.toString())
                )
                .build();

        DescribeInstanceInformationResponse response = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(IntStream.range(1, 50).mapToObj(index -> InstanceInformation.builder()
                        .ipAddress("127.0.0." + index)
                        .instanceId("host-" + index)
                        .platformType(PlatformType.LINUX)
                        .build()).collect(Collectors.toList()))
                .build();
        DescribeInstanceInformationIterable iterable = new DescribeInstanceInformationIterable(ssm, firstRequest);
        doReturn(iterable).when(ssm).describeInstanceInformationPaginator(any(Consumer.class));
        doReturn(response).when(ssm).describeInstanceInformation(eq(firstRequest));

        ProvisionOutput result = CompletableFuture.supplyAsync(() -> {
            ProvisionOutput output = provisionService.provision(ProvisionInput.builder().amount(10).build());
            while (!output.status().isTerminal()) {
                output = provisionService.describe(output);
            }
            return output;
        }).get(5, TimeUnit.SECONDS);

        Set<String> possibleIds = response.instanceInformationList().stream()
                .map(InstanceInformation::instanceId)
                .collect(Collectors.toSet());

        assertEquals(10, result.reservations().size());
        assertTrue(possibleIds.containsAll(result.reservations().stream()
                .map(Reservation::deviceId)
                .collect(Collectors.toSet())));

        ProvisionOutput another = CompletableFuture.supplyAsync(() -> {
            ProvisionOutput output = provisionService.provision(ProvisionInput.builder().amount(60).build());
            while (!output.status().isTerminal()) {
                output = provisionService.describe(output);
            }
            return output;
        }).get(5, TimeUnit.SECONDS);
        assertEquals(Status.FAILED, another.status());
    }

    @Test
    void GIVEN_provision_service_WHEN_exchanging_reservation_THEN_ssm_instance_is_used() {
        DescribeInstanceInformationResponse response = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.instanceId("abc-123").platformType(PlatformType.LINUX).ipAddress("127.0.0.1")
                )
                .build();
        doReturn(response).when(ssm).describeInstanceInformation(eq(DescribeInstanceInformationRequest.builder()
                .filters(
                        filter -> filter.key("InstanceIds").values("abc-123")
                )
                .build()));
        Host expected = Host.builder()
                .deviceId("abc-123")
                .port(22)
                .platform(PlatformOS.of("Linux", "armv6"))
                .hostName("127.0.0.1")
                .build();
        assertEquals(expected, provisionService.exchange(Reservation.of("abc-123", Status.SUCCEEDED)));

        doReturn(DescribeInstanceInformationResponse.builder()
                .instanceInformationList(Collections.EMPTY_LIST)
                .build())
                .when(ssm).describeInstanceInformation(eq(DescribeInstanceInformationRequest.builder()
                .filters(
                        filter -> filter.key("InstanceIds").values("non-exist")
                )
                .build()));

        assertThrows(ReservationException.class, () -> provisionService.exchange(Reservation.of("non-exist", Status.SUCCEEDED)));
    }

    @Test
    void GIVEN_provision_service_WHEN_closing_THEN_ssm_is_closed() {
        provisionService.close();
        verify(ssm).close();
    }

    @Test
    void GIVEN_no_service_WHEN_config_is_used_THEN_service_is_created() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader());
        ProvisionServiceSSM service = ProvisionServiceSSM.builder().ssm(ssm).fromConfig(config);
        assertEquals("armv7", service.architecture());
    }

    @Test
    void GIVEN_no_factory_WHEN_config_is_empty_THEN_create_default() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("devices/pool.properties"));
        properties.clear();
        DevicePoolConfig config = DevicePoolConfigProperties.load(properties);
        assertThrows(ProvisioningException.class, () -> ProvisionServiceSSM.builder().ssm(ssm).fromConfig(config));
    }
}
