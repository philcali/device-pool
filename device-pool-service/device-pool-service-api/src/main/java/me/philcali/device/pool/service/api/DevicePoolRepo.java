package me.philcali.device.pool.service.api;


import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;

public interface DevicePoolRepo {
    DevicePoolObject get(CompositeKey compositeKey, String poolId) throws NotFoundException;

    QueryResults<DevicePoolRepo> list(CompositeKey compositeKey, QueryParams params);

    void delete(CompositeKey compositeKey, String poolId);
}
