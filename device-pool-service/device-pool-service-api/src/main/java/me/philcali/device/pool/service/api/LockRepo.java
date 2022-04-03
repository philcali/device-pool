/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.UpdateLockObject;

/**
 * <p>LockRepo interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface LockRepo extends ObjectRepository<LockObject, CreateLockObject, UpdateLockObject> {
    /** Constant <code>SINGLETON="singleton"</code> */
    String SINGLETON = "singleton";

    /**
     * <p>get.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @return a {@link me.philcali.device.pool.service.api.model.LockObject} object
     * @throws me.philcali.device.pool.service.api.exception.NotFoundException if any.
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
    default LockObject get(CompositeKey account) throws NotFoundException, ServiceException {
        return get(account, SINGLETON);
    }

    /**
     * Extends a lock by attempting to first lock the resource, and then updating the expires time
     * if the lock is held. Note that if the holder of the lock is not the caller, then an
     * {@link me.philcali.device.pool.service.api.exception.NotFoundException} will be propagated.
     *
     * @param resourceKey the underlying unique identifier for the resource being locked
     * @param put the request to either extend or lock the resource
     * @return the created or modified {@link me.philcali.device.pool.service.api.model.LockObject}
     * @throws me.philcali.device.pool.service.api.exception.NotFoundException if on managing the lock.
     * @throws me.philcali.device.pool.service.api.exception.ServiceException problem with accessing the service.
     */
    default LockObject extend(CompositeKey resourceKey, CreateLockObject put)
            throws InvalidInputException, ServiceException {
        try {
            return create(resourceKey, CreateLockObject.builder()
                    .holder(put.holder())
                    .value(put.value())
                    .expiresIn(put.expiresIn())
                    .duration(put.duration())
                    .build());
        } catch (ConflictException e) {
            return update(resourceKey, UpdateLockObject.builder()
                    .holder(put.holder())
                    .value(put.value())
                    .expiresIn(put.expiresIn())
                    .build());
        }
    }
}
