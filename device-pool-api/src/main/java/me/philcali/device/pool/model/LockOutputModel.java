/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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
