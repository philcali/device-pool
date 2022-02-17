/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = Error.class)
interface ErrorModel {
    /**
     * <p>errorMessage.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String errorMessage();

    /**
     * <p>errorType.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String errorType();

    /**
     * <p>stackTrace.</p>
     *
     * @return a {@link java.util.List} object
     */
    @Value.Default
    default List<String> stackTrace() {
        return Collections.emptyList();
    }
}
