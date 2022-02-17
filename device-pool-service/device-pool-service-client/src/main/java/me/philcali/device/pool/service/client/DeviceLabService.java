/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
import me.philcali.device.pool.service.api.model.UpdateLockObject;
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * <p>DeviceLabService interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface DeviceLabService {
    /**
     * <p>listDevicePools.</p>
     *
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools")
    Call<QueryResults<DevicePoolObject>> listDevicePools(
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    /**
     * <p>getDevicePool.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}")
    Call<DevicePoolObject> getDevicePool(
            @Path("poolId") String poolId);

    /**
     * <p>createDevicePool.</p>
     *
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateDevicePoolObject} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools")
    Call<DevicePoolObject> createDevicePool(
            @Body CreateDevicePoolObject create);

    /**
     * <p>updateDevicePool.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param update a {@link me.philcali.device.pool.service.api.model.UpdateDevicePoolObject} object
     * @return a {@link retrofit2.Call} object
     */
    @PUT("pools/{poolId}")
    Call<DevicePoolObject> updateDevicePool(
            @Path("poolId") String poolId,
            @Body UpdateDevicePoolObject update);

    /**
     * <p>deleteDevicePool.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @DELETE("pools/{poolId}")
    Call<Void> deleteDevicePool(
            @Path("poolId") String poolId);

    /**
     * <p>listDevices.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/devices")
    Call<QueryResults<DeviceObject>> listDevices(
            @Path("poolId") String poolId,
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    /**
     * <p>getDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/devices/{deviceId}")
    Call<DeviceObject> getDevice(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId);

    /**
     * <p>createDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateDeviceObject} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools/{poolId}/devices")
    Call<DeviceObject> createDevice(
            @Path("poolId") String poolId,
            @Body CreateDeviceObject create);

    /**
     * <p>updateDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param update a {@link me.philcali.device.pool.service.api.model.UpdateDeviceObject} object
     * @return a {@link retrofit2.Call} object
     */
    @PUT("pools/{poolId}/devices/{deviceId}")
    Call<DeviceObject> updateDevice(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId,
            @Body UpdateDeviceObject update);

    /**
     * <p>deleteDevice.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @DELETE("pools/{poolId}/devices/{deviceId}")
    Call<Void> deleteDevice(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId);

    /**
     * <p>listProvisions.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/provisions")
    Call<QueryResults<ProvisionObject>> listProvisions(
            @Path("poolId") String poolId,
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    /**
     * <p>getProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/provisions/{provisionId}")
    Call<ProvisionObject> getProvision(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId);

    /**
     * <p>createProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateProvisionObject} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools/{poolId}/provisions")
    Call<ProvisionObject> createProvision(
            @Path("poolId") String poolId,
            @Body CreateProvisionObject create);

    /**
     * <p>cancelProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools/{poolId}/provisions/{provisionId}")
    Call<ProvisionObject> cancelProvision(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId);

    /**
     * <p>deleteProvision.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @DELETE("pools/{poolId}/provisions/{provisionId}")
    Call<Void> deleteProvision(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId);

    /**
     * <p>listReservations.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param nextToken a {@link java.lang.String} object
     * @param limit a {@link java.lang.Integer} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/provisions/{provisionId}/reservations")
    Call<QueryResults<ReservationObject>> listReservations(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId,
            @Query("nextToken") String nextToken,
            @Query("limit") Integer limit);

    /**
     * <p>getReservation.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param reservationId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/provision/{provisionId}/reservations/{reservationId}")
    Call<ReservationObject> getReservation(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId,
            @Path("reservationId") String reservationId);

    /**
     * <p>cancelReservation.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param reservationId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools/{poolId}/provision/{provisionId}/reservations/{reservationId}")
    Call<ReservationObject> cancelReservation(
            @Path("poolId") String poolId,
            @Path("provisionId") String provisionId,
            @Path("reservationId") String reservationId);

    /**
     * <p>getDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/locks")
    Call<LockObject> getDevicePoolLock(@Path("poolId") String poolId);

    /**
     * <p>getDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @GET("pools/{poolId}/devices/{deviceId}/locks")
    Call<LockObject> getDeviceLock(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId);

    /**
     * <p>createDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateLockObject} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools/{poolId}/locks")
    Call<LockObject> createDevicePoolLock(
            @Path("poolId") String poolId,
            @Body CreateLockObject create);

    /**
     * <p>createDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateLockObject} object
     * @return a {@link retrofit2.Call} object
     */
    @POST("pools/{poolId}/devices/{deviceId}/locks")
    Call<LockObject> createDeviceLock(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId,
            @Body CreateLockObject create);

    /**
     * <p>extendDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param update a {@link me.philcali.device.pool.service.api.model.UpdateLockObject} object
     * @return a {@link retrofit2.Call} object
     */
    @PUT("pools/{poolId}/locks")
    Call<LockObject> extendDevicePoolLock(
            @Path("poolId") String poolId,
            @Body UpdateLockObject update);

    /**
     * <p>extendDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param update a {@link me.philcali.device.pool.service.api.model.UpdateLockObject} object
     * @return a {@link retrofit2.Call} object
     */
    @PUT("pools/{poolId}/devices/{deviceId}/locks")
    Call<LockObject> extendDeviceLock(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId,
            @Body UpdateLockObject update);

    /**
     * <p>releaseDevicePoolLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @DELETE("pools/{poolId}/locks")
    Call<Void> releaseDevicePoolLock(@Path("poolId") String poolId);

    /**
     * <p>releaseDeviceLock.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @return a {@link retrofit2.Call} object
     */
    @DELETE("pools/{poolId}/devices/{deviceId}/locks")
    Call<Void> releaseDeviceLock(
            @Path("poolId") String poolId,
            @Path("deviceId") String deviceId);


    /**
     * <p>listDevicePools.</p>
     *
     * @param params a {@link me.philcali.device.pool.service.api.model.QueryParams} object
     * @return a {@link retrofit2.Call} object
     */
    default Call<QueryResults<DevicePoolObject>> listDevicePools(QueryParams params) {
        return listDevicePools(params.nextToken(), params.limit());
    }

    /**
     * <p>listDevices.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param params a {@link me.philcali.device.pool.service.api.model.QueryParams} object
     * @return a {@link retrofit2.Call} object
     */
    default Call<QueryResults<DeviceObject>> listDevices(String poolId, QueryParams params) {
        return listDevices(poolId, params.nextToken(), params.limit());
    }

    /**
     * <p>listProvisions.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param params a {@link me.philcali.device.pool.service.api.model.QueryParams} object
     * @return a {@link retrofit2.Call} object
     */
    default Call<QueryResults<ProvisionObject>> listProvisions(String poolId, QueryParams params) {
        return listProvisions(poolId, params.nextToken(), params.limit());
    }

    /**
     * <p>listReservations.</p>
     *
     * @param poolId a {@link java.lang.String} object
     * @param provisionId a {@link java.lang.String} object
     * @param params a {@link me.philcali.device.pool.service.api.model.QueryParams} object
     * @return a {@link retrofit2.Call} object
     */
    default Call<QueryResults<ReservationObject>> listReservations(
            String poolId,
            String provisionId,
            QueryParams params) {
        return listReservations(poolId, provisionId, params.nextToken(), params.limit());
    }

    /**
     * <p>defaultClientBuilder.</p>
     *
     * @return a {@link java.util.function.BiConsumer} object
     */
    static BiConsumer<OkHttpClient.Builder, Retrofit.Builder> defaultClientBuilder() {
        return (clientBuilder, retrofit) -> {
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
            clientBuilder.callTimeout(callTimeout)
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .writeTimeout(writeTimeout);
        };
    }

    /**
     * <p>create.</p>
     *
     * @param thunk a {@link java.util.function.BiConsumer} object
     * @return a {@link me.philcali.device.pool.service.client.DeviceLabService} object
     */
    static DeviceLabService create(BiConsumer<OkHttpClient.Builder, Retrofit.Builder> thunk) {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        retrofitBuilder.addConverterFactory(JacksonConverterFactory.create(mapper));
        defaultClientBuilder().andThen(thunk).accept(clientBuilder, retrofitBuilder);
        Retrofit retrofit = retrofitBuilder.client(clientBuilder.build()).build();
        return retrofit.create(DeviceLabService.class);
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link me.philcali.device.pool.service.client.DeviceLabService} object
     */
    static DeviceLabService create() {
        return create((client, builder) -> {
            String baseUrl = System.getenv("DEVICE_LAB_SERVICE_ENDPOINT");
            if (Objects.nonNull(baseUrl) && !baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            client.addInterceptor(AwsV4SigningInterceptor.create());
            builder.baseUrl(baseUrl);
        });
    }
}
