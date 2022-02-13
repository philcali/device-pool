/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.lab;

import me.philcali.device.pool.example.Lab;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolLockOptions;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.client.DeviceLabService;
import picocli.CommandLine;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Objects;

@CommandLine.Command(
        name = "init",
        description = "Sets up a managed device pool with devices"
)
public class Init implements Runnable {
    @CommandLine.ParentCommand
    Lab deviceLab;

    @CommandLine.Option(
            names = {"-n", "--host"},
            required = true,
            description = "IP addresses of the hosts representing this pool")
    String[] hostNames;

    private <T> T execute(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (Objects.nonNull(response.errorBody())) {
                throw new RuntimeException(response.errorBody().string());
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        DeviceLabService service = deviceLab.createService();

        Call<DevicePoolObject> createPool = service.createDevicePool(CreateDevicePoolObject.builder()
                .name(deviceLab.poolName())
                .type(DevicePoolType.MANAGED)
                .description("This is an example device pool")
                .lockOptions(DevicePoolLockOptions.of(true))
                .build());
        DevicePoolObject createdPool = execute(createPool);
        System.out.println("Created device pool " + createdPool.name());

        for (int i = 0; i < hostNames.length; i++) {
            String hostName = hostNames[i];
            Call<DeviceObject> createDevice = service.createDevice(createdPool.id(), CreateDeviceObject.builder()
                    .publicAddress(hostName)
                    .id("host-" + i)
                    .build());
            DeviceObject device = execute(createDevice);
            System.out.println("Created device " + device.id() + " for pool " + createdPool.id());
        }
        System.out.println("Device pool " + createdPool.id() + " is all set!");
    }
}
