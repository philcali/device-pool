package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.model.CreateDeviceLockObject;
import me.philcali.device.pool.service.api.model.DeviceLockObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceLockObject;

public interface DeviceLockRepo
        extends ObjectRepository<DeviceLockObject, CreateDeviceLockObject, UpdateDeviceLockObject> {
}
