package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@ApiModel
@Value.Immutable
interface QueryResultsModel<T> {
    List<T> results();

    @Nullable
    String nextToken();

    @Value.Default
    default boolean isTruncated() {
        return Objects.nonNull(nextToken());
    }
}
