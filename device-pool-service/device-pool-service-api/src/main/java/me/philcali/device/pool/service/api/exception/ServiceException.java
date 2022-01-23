/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.exception;

public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 3690631849592145324L;

    public ServiceException(Throwable ex) {
        super(ex);
    }
}
