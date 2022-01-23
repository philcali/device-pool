/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

public enum Status {
    REQUESTED(false),
    PROVISIONING(false),
    CANCELING(false),
    CANCELED(true),
    SUCCEEDED(true),
    FAILED(true);

    private boolean terminal;

    Status(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
