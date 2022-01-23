/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = QueryResults.class)
interface QueryResultsModel<T> {
    List<T> results();

    @Nullable
    String nextToken();

    @Value.Default
    default boolean isTruncated() {
        return Objects.nonNull(nextToken());
    }
}
