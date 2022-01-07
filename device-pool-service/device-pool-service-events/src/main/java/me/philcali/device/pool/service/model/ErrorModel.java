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
    String errorMessage();

    @Nullable
    String errorType();

    @Value.Default
    default List<String> stackTrace() {
        return Collections.emptyList();
    }
}
