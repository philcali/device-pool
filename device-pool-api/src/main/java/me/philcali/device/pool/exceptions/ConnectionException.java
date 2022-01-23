/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.exceptions;

import me.philcali.device.pool.model.CommandInput;

public class ConnectionException extends RuntimeException {
    private static final long serialVersionUID = -8046163299450391791L;
    private int errorCode = -1;
    private CommandInput originalInput;

    public ConnectionException(Throwable ex) {
        super(ex);
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(int exitCode, String message, CommandInput originalInput) {
        super(message);
        this.errorCode = exitCode;
        this.originalInput = originalInput;
    }

    public int errorCode() {
        return errorCode;
    }

    public CommandInput originalInput() {
        return originalInput;
    }
}
