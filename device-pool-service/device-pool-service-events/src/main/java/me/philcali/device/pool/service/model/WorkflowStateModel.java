/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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
    abstract String executionArn();

    @Nullable
    @Value.Default
    String executionId() {
        if (Objects.isNull(executionArn())) {
            return null;
        }
        return executionArn().substring(executionArn().lastIndexOf(':'));
    }

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

    /**
     * <p>normalizedError.</p>
     *
     * @param mapper a {@link com.fasterxml.jackson.databind.ObjectMapper} object
     * @return a {@link me.philcali.device.pool.service.model.Error} object
     * @throws com.fasterxml.jackson.core.JsonProcessingException if any.
     */
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

    /**
     * <p>fail.</p>
     *
     * @return a {@link me.philcali.device.pool.service.model.WorkflowState} object
     */
    @JsonIgnore
    public WorkflowState fail() {
        return fail(errorMessage());
    }

    /**
     * <p>fail.</p>
     *
     * @param error a {@link java.lang.String} object
     * @return a {@link me.philcali.device.pool.service.model.WorkflowState} object
     */
    public WorkflowState fail(String error) {
        return update(b -> b.status(Status.FAILED).message(error));
    }

    /**
     * <p>update.</p>
     *
     * @param thunk a {@link java.util.function.Consumer} object
     * @return a {@link me.philcali.device.pool.service.model.WorkflowState} object
     */
    public WorkflowState update(Consumer<ProvisionObject.Builder> thunk) {
        ProvisionObject.Builder builder = ProvisionObject.builder().from(provision()).key(key());
        thunk.accept(builder);
        return WorkflowState.builder()
                .from(this)
                .provision(builder.build())
                .build();
    }
}
