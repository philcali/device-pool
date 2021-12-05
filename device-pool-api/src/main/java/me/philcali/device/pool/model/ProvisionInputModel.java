package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.UUID;

@ApiModel
@Value.Immutable
interface ProvisionInputModel {
    @Value.Default
    default String id() {
        return UUID.randomUUID().toString();
    }

    @Value.Default
    default int amount() {
        return 1;
    }
}
