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
