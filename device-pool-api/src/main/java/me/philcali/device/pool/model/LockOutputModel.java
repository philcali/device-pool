package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
interface LockOutputModel {
    String id();

    @Nullable
    String value();

    long updatedAt();

    long expiresIn();
}
