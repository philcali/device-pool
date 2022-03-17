/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import java.util.function.Function;

public interface OperationFunction<I, O> extends Function<I, O> {
    Class<I> inputType();
}
