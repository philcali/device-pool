package me.philcali.device.pool.service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = WorkflowState.class)
abstract class WorkflowStateModel {
    abstract CompositeKey key();

    abstract ProvisionObject provision();

    @Nullable
    abstract DevicePoolEndpoint endpoint();

    @Value.Default
    boolean done() {
        return false;
    }

    @Nullable
    abstract String error();

    @Value.Check
    WorkflowStateModel validate() {
        return WorkflowState.builder()
                .from(this)
                .provision(ProvisionObject.builder()
                        .from(provision())
                        .key(key())
                        .build())
                .build();
    }

    public WorkflowState fail() {
        return fail(error());
    }

    public WorkflowState fail(String message) {
        return update(b -> b.status(Status.FAILED).message(message));
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
