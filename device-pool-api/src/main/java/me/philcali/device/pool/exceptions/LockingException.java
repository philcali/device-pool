/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

public class LockingException extends RuntimeException {
    private static final long serialVersionUID = 6047014952839580825L;

    public LockingException(Throwable ex) {
        super(ex);
    }
}
