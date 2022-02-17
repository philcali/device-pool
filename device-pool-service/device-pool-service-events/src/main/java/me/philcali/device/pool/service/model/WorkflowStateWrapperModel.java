/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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
    /**
     * <p>input.</p>
     *
     * @return a {@link me.philcali.device.pool.service.model.WorkflowState} object
     */
    WorkflowState input();

    /**
     * <p>executionName.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Nullable
    String executionName();

    /**
     * <p>validate.</p>
     *
     * @return a {@link me.philcali.device.pool.service.model.WorkflowStateWrapperModel} object
     */
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
