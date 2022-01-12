package me.philcali.device.pool.client;

import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.client.DeviceLabService;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class DeviceLabProvisionServiceTest {
    private DeviceLabProvisionService service;

    @Mock
    private DeviceLabService underlyingService;

    @BeforeEach
    void setUp() {
        service = DeviceLabProvisionService.builder()
                .poolId("TestPool")
                .deviceLabService(underlyingService)
                .platform(PlatformOS.of("linux", "armv7"))
                .build();
    }

    @Test
    void GIVEN_device_lab_is_created_WHEN_lab_provisions_THEN_methods_are_called() throws Exception {
        CreateProvisionObject create = CreateProvisionObject.builder()
                .id("provision-123")
                .amount(1)
                .build();
        Call<ProvisionObject> createCall = mock(Call.class);
        when(underlyingService.createProvision(eq("TestPool"), eq(create))).thenReturn(createCall);

        Request createRequest = new Request.Builder()
                .url("http://example.com/pools/TestPool/provisions")
                .build();
        doReturn(createRequest).when(createCall).request();

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ProvisionObject createdProvision = ProvisionObject.builder()
                .id("provision-123")
                .status(Status.REQUESTED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(createCall.execute()).thenReturn(Response.success(createdProvision));

        ProvisionOutput provisionOutput = service.provision(ProvisionInput.builder()
                .id("provision-123")
                .amount(1)
                .build());

        ProvisionOutput expectedOutput = ProvisionOutput.builder()
                .id("provision-123")
                .status(Status.REQUESTED)
                .succeeded(false)
                .build();

        assertEquals(expectedOutput, provisionOutput);

        Call<ProvisionObject> canceledCall = mock(Call.class);
        when(underlyingService.cancelProvision(eq("TestPool"), eq("provision-123")))
                .thenReturn(canceledCall);
        doReturn(createRequest).when(canceledCall).request();
        doThrow(IOException.class).when(canceledCall).execute();

        service.close();
    }

    @Test
    void GIVEN_device_lab_is_created_WHEN_lab_describes_THEN_methods_are_called() throws IOException {
        Call<ProvisionObject> describeCall = mock(Call.class);
        when(underlyingService.getProvision(eq("TestPool"), eq("provision-123"))).thenReturn(describeCall);

        Request getRequest = new Request.Builder()
                .url("http://example.com/pools/TestPool/provisions/provision-123")
                .build();
        doReturn(getRequest).when(describeCall).request();

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ProvisionObject provisionObject = ProvisionObject.builder()
                .id("provision-123")
                .createdAt(now)
                .updatedAt(now)
                .status(Status.SUCCEEDED)
                .build();
        doReturn(Response.success(provisionObject)).when(describeCall).execute();

        Call<QueryResults<ReservationObject>> listCall = mock(Call.class);
        doCallRealMethod().when(underlyingService).listReservations(
                eq("TestPool"),
                eq("provision-123"),
                eq(QueryParams.builder()
                        .limit(100)
                        .build()));
        when(underlyingService.listReservations(eq("TestPool"), eq("provision-123"), eq(null), eq(100))).thenReturn(listCall);
        QueryResults<ReservationObject> listResults = QueryResults.<ReservationObject>builder()
                .addResults(ReservationObject.builder()
                        .deviceId("device-123")
                        .id("reservation-123")
                        .status(Status.SUCCEEDED)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .build();
        doReturn(Response.success(listResults)).when(listCall).execute();
        Request listRequest = new Request.Builder()
                .url("http://example.com/pools/TestPool/provisions/provision-123/reservations")
                .build();
        doReturn(listRequest).when(listCall).request();

        ProvisionOutput output = service.describe(ProvisionOutput.of("provision-123"));

        ProvisionOutput expectedOutput = ProvisionOutput.builder()
                .id("provision-123")
                .status(Status.SUCCEEDED)
                .addReservations(Reservation.builder()
                        .status(Status.SUCCEEDED)
                        .deviceId("device-123")
                        .build())
                .build();
        assertEquals(expectedOutput, output);
    }

    @Test
    void GIVEN_device_lab_is_created_WHEN_lab_exchange_THEN_device_is_exchanged() throws IOException {
        Call<DeviceObject> getCall = mock(Call.class);
        when(underlyingService.getDevice(eq("TestPool"), eq("device-123"))).thenReturn(getCall);

        Request getRequest = new Request.Builder()
                .url("http://example.com/pools/TestPool/devices/device-123")
                .build();
        doReturn(getRequest).when(getCall).request();

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DeviceObject deviceObject = DeviceObject.builder()
                .id("device-123")
                .createdAt(now)
                .updatedAt(now)
                .publicAddress("192.168.1.206")
                .build();
        doReturn(Response.success(deviceObject)).when(getCall).execute();

        Host host = service.exchange(Reservation.builder()
                .deviceId("device-123")
                .status(Status.SUCCEEDED)
                .build());

        Host expectedHost = Host.builder()
                .platform(service.platform())
                .deviceId("device-123")
                .hostName("192.168.1.206")
                .build();

        assertEquals(expectedHost, host);
    }
}
