/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = ErrorRaw.class)
interface ErrorRawModel {
    /**
     * <p>error.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @JsonProperty("Error")
    String error();

    /**
     * <p>cause.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @JsonProperty("Cause")
    String cause();
}
