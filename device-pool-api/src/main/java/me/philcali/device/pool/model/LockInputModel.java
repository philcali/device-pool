package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

@ApiModel
@Value.Immutable
interface LockInputModel {
    String id();

    @Nullable
    String value();

    @Value.Default
    default long ttl() {
        return TimeUnit.SECONDS.toMillis(10);
    }
}
