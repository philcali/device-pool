package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = UpdateDeviceObject.class)
interface UpdateProvisionObjectModel {
    @Nullable
    String id();

    Status status();

    @Nullable
    String message();

    @Nullable
    Instant expiresIn();
}
