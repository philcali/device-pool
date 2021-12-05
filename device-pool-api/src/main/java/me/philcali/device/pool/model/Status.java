package me.philcali.device.pool.model;

public enum Status {
    REQUESTED(false),
    PROVISIONING(false),
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
