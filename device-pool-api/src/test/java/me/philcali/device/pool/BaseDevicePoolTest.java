package me.philcali.device.pool;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ProvisionService;
import me.philcali.device.pool.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({MockitoExtension.class})
class BaseDevicePoolTest {
    private BaseDevicePool pool;

    @Mock
    private ProvisionService provisionService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private ConnectionFactory connections;
    @Mock
    private ContentTransferAgentFactory transfers;
    @Mock
    private DoubleService doubleService;
    @Mock
    private DoubleFactory doubleFactory;

    interface DoubleService extends ProvisionService, ReservationService {

    }

    interface DoubleFactory extends ConnectionFactory, ContentTransferAgentFactory {

    }

    @BeforeEach
    void setup() {
        pool = BaseDevicePool.builder()
                .transfers(transfers)
                .connections(connections)
                .provisionService(provisionService)
                .reservationService(reservationService)
                .build();
    }

    @Test
    void GIVEN_pool_is_created_WHEN_provision_sync_THEN_services_are_called() {
        ProvisionInput input = ProvisionInput.builder()
                .id("abc-efg")
                .amount(1)
                .build();

        Host expectedHost = Host.builder()
                .deviceId("device-1-abc-efg")
                .hostName("10.0.0.1")
                .port(22)
                .platform(PlatformOS.builder()
                        .os("Linux")
                        .arch("aarch64")
                        .build())
                .build();

        Connection connection = mock(Connection.class);
        ContentTransferAgent transfer = mock(ContentTransferAgent.class);

        ProvisionOutput expectedOutput = ProvisionOutput.builder()
                .id(input.id())
                .addReservations(Reservation.builder()
                        .deviceId("device-1-abc-efg")
                        .status(Status.PROVISIONING)
                        .build())
                .build();
        when(provisionService.provision(eq(input))).thenReturn(expectedOutput);

        when(provisionService.describe(eq(expectedOutput))).thenReturn(ProvisionOutput.builder()
                .id(input.id())
                .addReservations(Reservation.builder()
                        .deviceId("device-1-abc-efg")
                        .status(Status.SUCCEEDED)
                        .build())
                .build());

        when(reservationService.exchange(eq(Reservation.of("device-1-abc-efg", Status.SUCCEEDED)))).thenReturn(expectedHost);
        when(connections.connect(eq(expectedHost))).thenReturn(connection);
        when(transfers.connect(eq(input.id()), eq(connection), eq(expectedHost))).thenReturn(transfer);

        List<Device> devices = pool.provisionWait(input, 10, TimeUnit.SECONDS);

        Device expectedDevice = BaseDevice.builder()
                .host(expectedHost)
                .connection(connection)
                .contentTransfer(transfer)
                .build();
        assertEquals(expectedDevice, devices.get(0));
    }

    @Test
    void GIVEN_pool_is_created_WHEN_provision_sync_fails_THEN_exception_is_thrown() {
        ProvisionInput input = ProvisionInput.builder()
                .id("abc-efg")
                .amount(1)
                .build();

        ProvisionOutput expectedOutput = ProvisionOutput.builder()
                .id(input.id())
                .addReservations(Reservation.builder()
                        .deviceId("device-1-abc-efg")
                        .status(Status.PROVISIONING)
                        .build())
                .build();
        when(provisionService.provision(eq(input))).thenReturn(expectedOutput);

        when(provisionService.describe(eq(expectedOutput))).thenReturn(ProvisionOutput.builder()
                .id(input.id())
                .addReservations(Reservation.builder()
                        .deviceId("device-1-abc-efg")
                        .status(Status.FAILED)
                        .build())
                .build());

        assertThrows(ProvisioningException.class, () -> pool.provisionWait(input, 10, TimeUnit.SECONDS));
    }

    @Test
    void GIVEN_pool_is_created_WHEN_provision_sync_times_out_THEN_exception_is_thrown() {
        ProvisionInput input = ProvisionInput.builder()
                .id("abc-efg")
                .amount(1)
                .build();

        ProvisionOutput stuckOutput = ProvisionOutput.builder()
                .id(input.id())
                .addReservations(Reservation.builder()
                        .deviceId("device-1-abc-efg")
                        .status(Status.PROVISIONING)
                        .build())
                .build();

        when(provisionService.provision(eq(input))).thenReturn(stuckOutput);
        when(provisionService.describe(eq(stuckOutput))).thenReturn(stuckOutput);

        assertThrows(ProvisioningException.class, () -> pool.provisionWait(input, 10, TimeUnit.MILLISECONDS));
    }

    @Test
    void GIVEN_pool_is_created_WHEN_pool_closes_THEN_all_closeables_are_called() throws Exception {
        pool.close();

        verify(reservationService).close();
        verify(provisionService).close();
        verify(transfers).close();
        verify(connections).close();
    }

    @Test
    void GIVEN_special_pool_is_created_WHEN_pool_closes_THEN_all_closeables_are_called() throws Exception {
        pool = BaseDevicePool.builder()
                .connectionAndContentFactory(doubleFactory)
                .provisionAndReservationService(doubleService)
                .build();
        pool.close();

        verify(doubleService, times(2)).close();
        verify(doubleFactory, times(2)).close();
    }
}
