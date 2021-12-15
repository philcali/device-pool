package me.philcali.device.pool.service.api;


import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;

public interface ProvisionRepo {
    ProvisionObject get(CompositeKey compositeKey, String provisionId) throws NotFoundException;

    QueryResults<ProvisionObject> list(CompositeKey compositeKey, QueryParams params);

    void delete(CompositeKey compositeKey, String provisionId);
}
