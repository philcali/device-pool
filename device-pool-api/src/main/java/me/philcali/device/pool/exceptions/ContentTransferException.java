/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

/**
 * <p>ContentTransferException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class ContentTransferException extends RuntimeException {
    private static final long serialVersionUID = 7149765931329187911L;

    /**
     * <p>Constructor for ContentTransferException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public ContentTransferException(Throwable ex) {
        super(ex);
    }
}
