/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

/**
 * <p>Status class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public enum Status {
    REQUESTED(false),
    PROVISIONING(false),
    CANCELING(false),
    CANCELED(true),
    SUCCEEDED(true),
    FAILED(true);

    private final boolean terminal;

    Status(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * <p>isTerminal.</p>
     *
     * @return a boolean
     */
    public boolean isTerminal() {
        return terminal;
    }
}
