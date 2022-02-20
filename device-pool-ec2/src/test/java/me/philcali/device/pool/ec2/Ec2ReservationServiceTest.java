/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ec2;

import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class Ec2ReservationServiceTest {

    @Mock
    private Ec2Client ec2;
    private Ec2ReservationService service;
    private Reservation reservation;

    @BeforeEach
    void setup() {
        service = Ec2ReservationService.builder()
                .ec2(ec2)
                .proxyJump("proxy-host.amazon.com")
                .platform(PlatformOS.of("Linux", "aarch64"))
                .build();

        reservation = Reservation.builder()
                .status(Status.SUCCEEDED)
                .deviceId("i-asdfasdcer")
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void GIVEN_reservation_service_WHEN_exchange_is_made_THEN_host_is_provided() throws Exception {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(reservation.deviceId())
                .build();

        when(ec2.describeInstances(eq(describeInstancesRequest))).thenReturn(DescribeInstancesResponse.builder()
                .reservations(builder -> builder.instances(Instance.builder()
                        .publicIpAddress("10.0.6.1")
                        .instanceId(reservation.deviceId())
                        .build()))
                .build());

        Host expectedHost = Host.builder()
                .port(22)
                .hostName("10.0.6.1")
                .platform(service.platform())
                .proxyJump(service.proxyJump())
                .deviceId(reservation.deviceId())
                .build();

        Host host = service.exchange(reservation);
        assertEquals(expectedHost, host);

        service.close();
        verify(ec2).close();
    }

    @Test
    void GIVEN_reservation_service_WHEN_exchange_is_made_THEN_exception_is_thrown() {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(reservation.deviceId())
                .build();

        when(ec2.describeInstances(eq(describeInstancesRequest))).thenThrow(Ec2Exception.class);

        assertThrows(ReservationException.class, () -> service.exchange(reservation));
    }

    @Test
    void GIVEN_no_service_WHEN_config_is_used_THEN_service_is_created() throws IOException {
        DevicePoolConfig config = DevicePoolConfigProperties.load(getClass().getClassLoader());
        Ec2ReservationService service = Ec2ReservationService.builder().ec2(ec2).fromConfig(config);
        assertEquals(PlatformOS.of("unix", "armv7"), service.platform());
        assertEquals(8022, service.port());
        assertEquals("proxy-jump.example.com", service.proxyJump());
    }

    @Test
    void GIVEN_no_service_WHEN_config_is_missing_THEN_service_fails_to_create() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("devices/pool.properties"));
        properties.remove("device.pool.reservation.ec2.platform");
        DevicePoolConfig config = DevicePoolConfigProperties.load(properties);
        assertThrows(ReservationException.class, () -> Ec2ReservationService.builder()
                .ec2(ec2)
                .fromConfig(config));
    }
}
