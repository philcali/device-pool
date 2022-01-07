package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;

public interface DeviceRepo extends ObjectRepository<DeviceObject, CreateDeviceObject, UpdateDeviceObject> {

    default DeviceObject put(CompositeKey account, DeviceObject device) throws ServiceException {
        try {
            return update(account, UpdateDeviceObject.builder()
                    .id(device.id())
                    .expiresIn(device.expiresIn())
                    .publicAddress(device.publicAddress())
                    .privateAddress(device.privateAddress())
                    .build());
        } catch (InvalidInputException e) {
            return create(account, CreateDeviceObject.builder()
                    .id(device.id())
                    .expiresIn(device.expiresIn())
                    .privateAddress(device.privateAddress())
                    .publicAddress(device.publicAddress())
                    .build());
        }
    }
}
