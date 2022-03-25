/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.rpc.lambda;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import java.util.List;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = ErrorMessage.class)
interface ErrorMessageModel {
    String errorMessage();

    String errorType();

    List<String> stackTrace();
}
