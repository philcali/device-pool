package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CreateDeviceObject.class)
interface CreateDeviceObjectModel {
    String id();

    @Nullable
    String poolId();

    String privateAddress();

    String publicAddress();

    @Nullable
    Instant expiresIn();
}
