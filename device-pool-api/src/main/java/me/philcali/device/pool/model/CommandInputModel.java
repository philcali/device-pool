package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApiModel
@Value.Immutable
interface CommandInputModel {
    @Nullable
    byte[] input();

    String line();

    @Nullable
    List<String> args();

    @Value.Default
    default Duration timeout() {
        return Duration.ofSeconds(30);
    }
}
