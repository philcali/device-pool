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

class DelegatingHostProvider extends AbstractHostProvider {
    private final Set<Host> hosts;
    private final HostProvider hostProvider;

    DelegatingHostProvider(Set<Host> hosts, HostProvider hostProvider) {
        this.hosts = new HashSet<>(hosts);
        this.hostProvider = hostProvider;
        hostProvider.addListener((change, host) -> {
            if (change == HostChange.Add && this.hosts.add(host)) {
                trigger(HostChange.Add, host);
            } else if (change == HostChange.Remove && this.hosts.remove(host)) {
                trigger(HostChange.Remove, host);
            }
        });
        this.hosts.addAll(hostProvider.hosts());
    }

    @Override
    public void requestGrowth() {
        super.requestGrowth();
        hostProvider.requestGrowth();
    }

    @Override
    public Set<Host> hosts() {
        return Collections.unmodifiableSet(hosts);
    }
}
