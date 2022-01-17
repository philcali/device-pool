package me.philcali.device.pool.service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Objects;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = WorkflowStateWrapper.class)
interface WorkflowStateWrapperModel {
    WorkflowState input();

    @Nullable
    String executionName();

    @Value.Check
    default WorkflowStateWrapperModel validate() {
        if (Objects.isNull(executionName()) || Objects.nonNull(input().executionArn())) {
            return this;
        }
        return WorkflowStateWrapper.builder()
                .from(this)
                .input(WorkflowState.builder()
                        .from(input())
                        .executionArn(executionName())
                        .build())
                .build();
    }
}
