package me.philcali.device.pool.model;

import org.immutables.value.Value;

@Value.Style(
        jdkOnly = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true)
public @interface APIShadowModel {
}
