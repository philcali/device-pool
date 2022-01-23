/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.exception;

public class NotFoundException extends ClientException {
    private static final long serialVersionUID = -5840584128715164263L;

    public NotFoundException(String message) {
        super(message);
    }
}
