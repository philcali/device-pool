package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.Set;

@ApiModel
@Value.Immutable
interface CopyInputModel {
    String source();

    String destination();

    Set<CopyOption> options();
}
