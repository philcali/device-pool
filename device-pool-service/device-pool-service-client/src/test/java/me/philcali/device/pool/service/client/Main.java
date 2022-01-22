package me.philcali.device.pool.service.client;

import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public final class Main {
    private static <T> T executeAndLog(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        System.out.println("Response was successful: " + response.isSuccessful());
        System.out.println("Response status: " + response.code());
        System.out.println("Response body: " + response.body());
        return response.body();
    }

    public static void main(String[] args) throws IOException {
        DeviceLabService service = DeviceLabService.create();

        Call<DevicePoolObject> create = service.createDevicePool(CreateDevicePoolObject.builder()
                .description("From Integ")
                .type(DevicePoolType.MANAGED)
                .name("IntegPool")
                .build());
        DevicePoolObject created = executeAndLog(create);

        Call<QueryResults<DevicePoolObject>> list = service.listDevicePools(QueryParams.builder()
                .limit(10)
                .build());
        executeAndLog(list);

        Call<Void> delete = service.deleteDevicePool(created.id());
        executeAndLog(delete);

        System.exit(0);
    }
}
