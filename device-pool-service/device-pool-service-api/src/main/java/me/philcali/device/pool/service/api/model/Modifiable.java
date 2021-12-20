package me.philcali.device.pool.service.api.model;

import javax.annotation.Nullable;
import java.time.Instant;

interface Modifiable {
    @Nullable
    Instant createdAt();

    Instant updatedAt();
}
