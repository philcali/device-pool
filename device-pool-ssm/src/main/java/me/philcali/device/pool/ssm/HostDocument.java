/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.Host;

import java.util.function.Function;

/**
 * Converts a {@link me.philcali.device.pool.model.Host} detail to an SSM RunDocument.
 *
 * @author philcali
 * @version $Id: $Id
 */
@FunctionalInterface
public interface HostDocument extends Function<Host, String> {
    /** {@inheritDoc} */
    @Override
    String apply(Host host);
}
