package me.philcali.device.pool.service.client;

import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

public interface DeviceLabService {
    @GET("/pools")
    Call<QueryResults<DevicePoolObject>> listDevicePools(
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    @GET("/pools/{poolId}")
    Call<DevicePoolObject> getDevicePool(
            @Path("poolId") String poolId);

    @POST("/pools")
    Call<DevicePoolObject> createDevicePool(
            @Body CreateDevicePoolObject create);

    @PUT("/pools/{poolId}")
    Call<DevicePoolObject> updateDevicePool(
            @Path("poolId") String poolId,
            @Body UpdateDevicePoolObject update);

    @DELETE("/pools/{poolId}")
    void deleteDevicePool(
            @Path("poolId") String poolId);

    @GET("/pools/{poolId}/devices")
    Call<QueryResults<DeviceObject>> listDevices(
            @Path("poolId") String poolId,
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    @GET("/pools/{poolId}/devices/{deviceId}")
    Call<DeviceObject> getDevice(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId);

    @POST("/pools/{poolId}/devices")
    Call<DeviceObject> createDevice(
            @Path("poolId") String poolId,
            @Body CreateDeviceObject create);

    @PUT("/pools/{poolId}/devices/{deviceId}")
    Call<DeviceObject> updateDevice(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId,
            @Body UpdateDeviceObject update);

    @DELETE("/pools/{poolId}/devices/{deviceId}")
    void deleteDevice(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId);

    @GET("/pools/{poolId}/provisions")
    Call<QueryResults<ProvisionObject>> listProvisions(
            @Path("poolId") String poolId,
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    @GET("/pools/{poolId}/provisions/{provisionId}")
    Call<ProvisionObject> getProvision(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId);

    @POST("/pools/{poolId}/provisions")
    Call<ProvisionObject> createProvision(
            @Path("poolId") String poolId,
            @Body CreateProvisionObject create);

    @POST("/pools/{poolId}/provisions/{provisionId}")
    Call<ProvisionObject> cancelProvision(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId);

    @DELETE("/pools/{poolId}/provisions/{provisionId}")
    void deleteProvision(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId);

    @GET("/pools/{poolId}/provisions/{provisionId}/reservations")
    Call<QueryResults<ReservationObject>> listReservations(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId,
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    @GET("/pools/{poolId}/provision/{provisionId}/reservations/{reservationId}")
    Call<ReservationObject> getReservation(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId,
            @Path("reservationId") String reservationId);

    @POST("/pools/{poolId}/provision/{provisionId}/reservations/{reservationId}")
    Call<ReservationObject> cancelReservation(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId,
            @Path("reservationId") String reservationId);

    default Call<QueryResults<DevicePoolObject>> listDevicePools(QueryParams params) {
        return listDevicePools(params.nextToken(), params.limit());
    }

    default Call<QueryResults<DeviceObject>> listDevices(String poolId, QueryParams params) {
        return listDevices(poolId, params.nextToken(), params.limit());
    }

    default Call<QueryResults<ProvisionObject>> listProvisions(String poolId, QueryParams params) {
        return listProvisions(poolId, params.nextToken(), params.limit());
    }

    default Call<QueryResults<ReservationObject>> listReservations(
            String poolId,
            String provisionId,
            QueryParams params) {
        return listReservations(poolId, provisionId, params.nextToken(), params.limit());
    }

    static Consumer<Retrofit.Builder> defaultClientBuilder() {
        return builder -> {
            final Duration callTimeout = Duration.ofSeconds(Optional.ofNullable(System.getenv("DEVICE_LAB_CALL_TIMEOUT"))
                    .map(Integer::parseInt)
                    .orElse(0));
            final Duration connectTimeout = Duration.ofSeconds(Optional.ofNullable(System.getenv("DEVICE_LAB_CONNECT_TIMEOUT"))
                    .map(Integer::parseInt)
                    .orElse(3));
            final Duration readTimeout = Duration.ofSeconds(Optional.ofNullable(System.getenv("DEVICE_LAB_READ_TIMEOUT"))
                    .map(Integer::parseInt)
                    .orElse(30));
            final Duration writeTimeout = Duration.ofSeconds(Optional.ofNullable(System.getenv("DEVICE_LAB_WRITE_TIMEOUT"))
                    .map(Integer::parseInt)
                    .orElse(20));
            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(callTimeout)
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .writeTimeout(writeTimeout)
                    .build();
            builder.client(client);
        };
    }

    static DeviceLabService create(Consumer<Retrofit.Builder> thunk) {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        defaultClientBuilder().andThen(thunk).accept(retrofitBuilder);
        Retrofit retrofit = retrofitBuilder.build();
        return retrofit.create(DeviceLabService.class);
    }

    static DeviceLabService create() {
        return create(builder -> {
            builder.addConverterFactory(JacksonConverterFactory.create())
                    .baseUrl(System.getenv("DEVICE_LAB_SERVICE_ENDPOINT"));
        });
    }
}
