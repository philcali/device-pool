/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.model.Host;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LocalHostProvider extends AbstractHostProvider implements HostProvider {
    private final Set<Host> hosts;
    private final Lock mutateLock;

    public LocalHostProvider(final Set<Host> hosts) {
        this.hosts = Collections.synchronizedSet(hosts);
        this.mutateLock = new ReentrantLock();
    }

    public static LocalHostProvider create() {
        return new LocalHostProvider(new HashSet<>());
    }

    public LocalHostProvider addHost(Host host) {
        mutateLock.lock();
        try {
            if (hosts.add(host)) {
                trigger(HostChange.Add, host);
            }
        } finally {
            mutateLock.unlock();
        }
        return this;
    }

    public LocalHostProvider removeHost(Host host) {
        mutateLock.lock();
        try {
            if (hosts.remove(host)) {
                trigger(HostChange.Remove, host);
            }
        } finally {
            mutateLock.unlock();
        }
        return this;
    }

    public LocalHostProvider reset(Set<Host> newValue) {
        mutateLock.lock();
        try {
            final Set<Host> diff = new HashSet<>(newValue);
            final Set<Host> existing = new HashSet<>(hosts);
            diff.removeAll(hosts);
            existing.removeAll(newValue);
            // Trigger removal of all "left over" hosts.
            existing.forEach(host -> trigger(HostChange.Remove, host));
            // Trigger addition to all "non-overlapping" hosts.
            diff.forEach(host -> trigger(HostChange.Add, host));
            hosts.clear();
            hosts.addAll(newValue);
        } finally {
            mutateLock.unlock();
        }
        return this;
    }

    @Override
    public Set<Host> hosts() {
        return Collections.unmodifiableSet(hosts);
    }
}
