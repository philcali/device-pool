/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.exception;

public class RetryableException extends RuntimeException {
    private static final long serialVersionUID = 3374954329983921116L;

    public RetryableException(Throwable ex) {
        super(ex);
    }

    public RetryableException(String message) {
        super(message);
    }
}
