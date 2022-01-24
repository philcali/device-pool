/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.api.model;

import javax.annotation.Nullable;
import java.time.Instant;

interface Modifiable {
    @Nullable
    Instant createdAt();

    Instant updatedAt();
}
