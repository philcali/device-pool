/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.model.Host;

import java.util.Set;
import java.util.function.BiConsumer;

public interface HostProvider {
    enum HostChange {
        Add, Remove
    }

    Set<Host> hosts();

    default void requestGrowth() {
        // No-op
    }

    void addListener(BiConsumer<HostChange, Host> consumer);

    void removeListener(BiConsumer<HostChange, Host> consumer);
}
