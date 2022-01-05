package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;

public interface DeviceRepo extends ObjectRepository<DeviceObject, CreateDeviceObject, UpdateDeviceObject> {
}
