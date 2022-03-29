/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.model.Host;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public abstract class AbstractHostProvider implements HostProvider {
    private final Set<BiConsumer<HostChange, Host>> listeners = new HashSet<>();

    protected void trigger(HostChange changeType, Host host) {
        listeners.forEach(listener -> listener.accept(changeType, host));
    }

    @Override
    public void addListener(BiConsumer<HostChange, Host> consumer) {
        listeners.add(consumer);
    }

    @Override
    public void removeListener(BiConsumer<HostChange, Host> consumer) {
        listeners.remove(consumer);
    }
}