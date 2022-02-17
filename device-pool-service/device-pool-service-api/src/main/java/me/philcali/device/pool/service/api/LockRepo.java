/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api;

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
}
