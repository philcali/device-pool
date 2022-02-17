/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
interface QueryParamsModel {
    /**
     * <p>limit.</p>
     *
     * @return a int
     */
    int limit();

    /**
     * <p>nextToken.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String nextToken();
}
