/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.exception;

/**
 * <p>ConflictException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class ConflictException extends ClientException {
    private static final long serialVersionUID = -4171506922223538482L;

    /**
     * <p>Constructor for ConflictException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public ConflictException(String message) {
        super(message);
    }
}
