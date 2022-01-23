/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nullable;

public interface UniqueEntity {
    @Nullable
    @JsonIgnore
    CompositeKey key();

    String id();

    @JsonIgnore
    default CompositeKey selfKey() {
        if (key() == null) {
            return null;
        }
        return CompositeKey.builder()
                .from(key())
                .addResources(id())
                .build();
    }
}
