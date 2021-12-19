package me.philcali.device.pool.service.api;


import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;

public interface DevicePoolRepo {
    int MAX_ITEMS = 100;

    DevicePoolObject get(CompositeKey compositeKey, String poolId)
            throws NotFoundException, ServiceException;

    DevicePoolObject create(CompositeKey compositeKey, CreateDevicePoolObject create)
            throws ConflictException, ServiceException;

    DevicePoolObject update(CompositeKey compositeKey, UpdateDevicePoolObject update)
            throws NotFoundException, ServiceException;

    QueryResults<DevicePoolObject> list(CompositeKey compositeKey, QueryParams params)
            throws ServiceException;

    void delete(CompositeKey compositeKey, String poolId)
            throws ServiceException;
}
