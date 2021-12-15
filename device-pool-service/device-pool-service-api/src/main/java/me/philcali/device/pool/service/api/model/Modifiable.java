package me.philcali.device.pool.service.api.model;

import org.immutables.value.Value;

import java.time.Instant;

interface Modifiable {
    @Value.Default
    default Instant createdAt() {
        return Instant.now();
    }

    Instant updatedAt();
}
