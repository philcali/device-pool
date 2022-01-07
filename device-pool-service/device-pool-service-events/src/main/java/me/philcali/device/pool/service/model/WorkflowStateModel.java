package me.philcali.device.pool.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.DevicePoolLockOptions;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = WorkflowState.class)
abstract class WorkflowStateModel {
    abstract CompositeKey key();

    abstract ProvisionObject provision();

    @Nullable
    abstract DevicePoolType poolType();

    @Nullable
    abstract DevicePoolEndpoint endpoint();

    @Value.Default
    boolean done() {
        return false;
    }

    @Nullable
    abstract String errorMessage();

    @Nullable
    abstract ErrorRaw error();

    public Error normalizedError(ObjectMapper mapper) throws JsonProcessingException {
        if (Objects.isNull(error())) {
            return null;
        }
        return mapper.readValue(error().cause(), Error.class);
    }

    @Value.Check
    WorkflowStateModel validate() {
        if (Objects.isNull(provision().key())) {
            return WorkflowState.builder()
                    .from(this)
                    .provision(ProvisionObject.builder()
                            .from(provision())
                            .key(key())
                            .build())
                    .build();
        }
        return this;
    }

    @Nullable
    abstract DevicePoolLockOptions poolLockOptions();

    @JsonIgnore
    public WorkflowState fail() {
        return fail(errorMessage());
    }

    public WorkflowState fail(String error) {
        return update(b -> b.status(Status.FAILED).message(error));
    }

    public WorkflowState update(Consumer<ProvisionObject.Builder> thunk) {
        ProvisionObject.Builder builder = ProvisionObject.builder().from(provision()).key(key());
        thunk.accept(builder);
        return WorkflowState.builder()
                .from(this)
                .provision(builder.build())
                .build();
    }
}
