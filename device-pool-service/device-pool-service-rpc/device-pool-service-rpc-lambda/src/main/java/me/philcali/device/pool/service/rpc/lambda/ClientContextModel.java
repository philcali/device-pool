package me.philcali.device.pool.service.rpc.lambda;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.api.model.CompositeKey;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = ClientContext.class)
interface ClientContextModel {
    CompositeKey accountKey();

    String operationName();
}
