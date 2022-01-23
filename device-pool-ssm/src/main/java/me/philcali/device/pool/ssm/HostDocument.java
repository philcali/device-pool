/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.Host;

import java.util.function.Function;

@FunctionalInterface
public interface HostDocument extends Function<Host, String> {
    @Override
    String apply(Host host);
}
