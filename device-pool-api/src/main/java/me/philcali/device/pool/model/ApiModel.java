/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

@Value.Style(
        jdkOnly = true,
        typeImmutable = "*",
        typeAbstract = "*Model",
        visibility = Value.Style.ImplementationVisibility.PUBLIC,
        allMandatoryParameters = true)
public @interface ApiModel {
}
