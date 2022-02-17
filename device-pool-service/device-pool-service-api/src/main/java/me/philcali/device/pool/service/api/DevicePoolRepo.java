/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;

import java.util.function.Consumer;

/**
 * <p>DevicePoolRepo interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface DevicePoolRepo
        extends ObjectRepository<DevicePoolObject, CreateDevicePoolObject, UpdateDevicePoolObject> {
    /**
     * <p>create.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param thunk a {@link java.util.function.Consumer} object
     * @return a {@link me.philcali.device.pool.service.api.model.DevicePoolObject} object
     */
    default DevicePoolObject create(CompositeKey account, Consumer<CreateDevicePoolObject.Builder> thunk) {
        CreateDevicePoolObject.Builder builder = CreateDevicePoolObject.builder();
        thunk.accept(builder);
        return create(account, builder.build());
    }

    /**
     * <p>update.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param thunk a {@link java.util.function.Consumer} object
     * @return a {@link me.philcali.device.pool.service.api.model.DevicePoolObject} object
     */
    default DevicePoolObject update(CompositeKey account, Consumer<UpdateDevicePoolObject.Builder> thunk) {
        UpdateDevicePoolObject.Builder builder = UpdateDevicePoolObject.builder();
        thunk.accept(builder);
        return update(account, builder.build());
    }
}
