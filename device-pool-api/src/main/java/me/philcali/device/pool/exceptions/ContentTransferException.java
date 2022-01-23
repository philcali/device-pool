/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

public class ContentTransferException extends RuntimeException {
    private static final long serialVersionUID = 7149765931329187911L;

    public ContentTransferException(Throwable ex) {
        super(ex);
    }
}
