package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;

public interface ObjectRepository<T, C, U> {
    int MAX_ITEMS = 100;

    T get(CompositeKey account, String id) throws NotFoundException, ServiceException;

    QueryResults<T> list(CompositeKey account, QueryParams params) throws ServiceException;

    T create(CompositeKey account, C create) throws ConflictException, ServiceException;

    T update(CompositeKey account, U update) throws NotFoundException, ServiceException;

    void delete(CompositeKey account, String id) throws ServiceException;
}
