/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;

/**
 * <p>ObjectRepository interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface ObjectRepository<T, C, U> {
    /** Constant <code>MAX_ITEMS=100</code> */
    int MAX_ITEMS = 100;

    /**
     * <p>get.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param id a {@link java.lang.String} object
     * @return a T object
     * @throws me.philcali.device.pool.service.api.exception.NotFoundException if any.
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
    T get(CompositeKey account, String id) throws NotFoundException, ServiceException;

    /**
     * <p>list.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param params a {@link me.philcali.device.pool.service.api.model.QueryParams} object
     * @return a {@link me.philcali.device.pool.service.api.model.QueryResults} object
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
    QueryResults<T> list(CompositeKey account, QueryParams params) throws ServiceException;

    /**
     * <p>create.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param create a C object
     * @return a T object
     * @throws me.philcali.device.pool.service.api.exception.ConflictException if any.
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
    T create(CompositeKey account, C create) throws ConflictException, ServiceException;

    /**
     * <p>update.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param update a U object
     * @return a T object
     * @throws me.philcali.device.pool.service.api.exception.NotFoundException if any.
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
    T update(CompositeKey account, U update) throws NotFoundException, ServiceException;

    /**
     * <p>delete.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param id a {@link java.lang.String} object
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
    void delete(CompositeKey account, String id) throws ServiceException;
}
