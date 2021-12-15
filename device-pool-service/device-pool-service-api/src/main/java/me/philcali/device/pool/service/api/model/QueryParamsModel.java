package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
interface QueryParamsModel {
    int limit();

    @Nullable
    String nextToken();
}
