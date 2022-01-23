/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data.exception;

public class TokenMarshallerException extends RuntimeException {
    private static final long serialVersionUID = -8454483268581110447L;

    public TokenMarshallerException(Throwable ex) {
        super(ex);
    }
}
