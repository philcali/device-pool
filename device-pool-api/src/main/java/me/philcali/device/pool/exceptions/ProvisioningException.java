/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

/**
 * <p>ProvisioningException class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class ProvisioningException extends RuntimeException {
    private static final long serialVersionUID = 8027212159281679219L;

    /**
     * <p>Constructor for ProvisioningException.</p>
     *
     * @param message a {@link java.lang.String} object
     */
    public ProvisioningException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for ProvisioningException.</p>
     *
     * @param ex a {@link java.lang.Throwable} object
     */
    public ProvisioningException(Throwable ex) {
        super(ex);
    }
}
