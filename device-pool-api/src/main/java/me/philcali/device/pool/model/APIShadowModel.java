/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

/**
 * A specific {@link org.immutables.value.Value.Immutable} helper that overshadows
 * builder implementations.
 *
 * @author philcali
 * @version $Id: $Id
 */
@Value.Style(
        jdkOnly = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true)
public @interface APIShadowModel {
}
