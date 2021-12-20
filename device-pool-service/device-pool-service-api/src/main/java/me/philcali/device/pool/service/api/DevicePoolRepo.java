package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;

import java.util.function.Consumer;

public interface DevicePoolRepo
        extends ObjectRepository<DevicePoolObject, CreateDevicePoolObject, UpdateDevicePoolObject> {
    int MAX_ITEMS = 100;

    default DevicePoolObject create(CompositeKey account, Consumer<CreateDevicePoolObject.Builder> thunk) {
        CreateDevicePoolObject.Builder builder = CreateDevicePoolObject.builder();
        thunk.accept(builder);
        return create(account, builder.build());
    }

    default DevicePoolObject update(CompositeKey account, Consumer<UpdateDevicePoolObject.Builder> thunk) {
        UpdateDevicePoolObject.Builder builder = UpdateDevicePoolObject.builder();
        thunk.accept(builder);
        return update(account, builder.build());
    }
}
