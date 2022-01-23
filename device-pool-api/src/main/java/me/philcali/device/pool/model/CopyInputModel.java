/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.immutables.value.Value;

import java.util.Collections;
import java.util.Set;

@ApiModel
@Value.Immutable
interface CopyInputModel {
    String source();

    String destination();

    @Value.Default
    default Set<CopyOption> options() {
        return Collections.emptySet();
    }
}
