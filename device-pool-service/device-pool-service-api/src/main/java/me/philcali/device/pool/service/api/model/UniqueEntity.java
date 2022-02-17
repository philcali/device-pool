/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nullable;

/**
 * <p>UniqueEntity interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface UniqueEntity {
    /**
     * <p>key.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     */
    @Nullable
    @JsonIgnore
    CompositeKey key();

    /**
     * <p>id.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String id();

    /**
     * <p>selfKey.</p>
     *
     * @return a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     */
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
