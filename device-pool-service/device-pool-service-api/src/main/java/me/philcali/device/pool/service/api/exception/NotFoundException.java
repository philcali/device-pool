/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.exception;

/**
 * <p>NotFoundException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class NotFoundException extends ClientException {
    private static final long serialVersionUID = -5840584128715164263L;

    /**
     * <p>Constructor for NotFoundException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public NotFoundException(String message) {
        super(message);
    }
}
