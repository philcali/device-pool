package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;

@ApiModel
@Value.Immutable
@JsonSerialize(as = ProvisionObject.class)
abstract class ProvisionObjectModel implements Modifiable, UniqueEntity {
    abstract String id();

    abstract Status status();

    @Nullable
    abstract Instant expiresIn();

    @Nullable
    abstract String message();
}
