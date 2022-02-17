/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;

/**
 * <p>DeviceRepo interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface DeviceRepo extends ObjectRepository<DeviceObject, CreateDeviceObject, UpdateDeviceObject> {

    /**
     * <p>put.</p>
     *
     * @param account a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param device a {@link me.philcali.device.pool.service.api.model.DeviceObject} object
     * @return a {@link me.philcali.device.pool.service.api.model.DeviceObject} object
     * @throws me.philcali.device.pool.service.api.exception.ServiceException if any.
     */
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
