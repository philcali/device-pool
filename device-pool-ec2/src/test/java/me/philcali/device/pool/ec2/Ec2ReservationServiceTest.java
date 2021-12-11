package me.philcali.device.pool.ec2;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
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
                .port(8080)
                .hostAddress(Instance::privateIpAddress)
                .platformOS(PlatformOS.of("Linux", "aarch64"))
                .build();

        reservation = Reservation.builder()
                .status(Status.SUCCEEDED)
                .deviceId("i-asdfasdcer")
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void GIVEN_reservation_service_WHEN_exchange_is_made_THEN_host_is_provided() {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(reservation.deviceId())
                .build();

        when(ec2.describeInstances(eq(describeInstancesRequest))).thenReturn(DescribeInstancesResponse.builder()
                .reservations(builder -> builder.instances(Instance.builder()
                        .privateIpAddress("10.0.6.1")
                        .instanceId(reservation.deviceId())
                        .build()))
                .build());

        Host expectedHost = Host.builder()
                .port(8080)
                .hostName("10.0.6.1")
                .platform(service.platformOS())
                .proxyJump(service.proxyJump())
                .deviceId(reservation.deviceId())
                .build();

        Host host = service.exchange(reservation);
        assertEquals(expectedHost, host);
    }

    @Test
    void GIVEN_reservation_service_WHEN_exchange_is_made_THEN_exception_is_thrown() {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(reservation.deviceId())
                .build();

        when(ec2.describeInstances(eq(describeInstancesRequest))).thenThrow(Ec2Exception.class);

        assertThrows(ReservationException.class, () -> service.exchange(reservation));
    }
}
