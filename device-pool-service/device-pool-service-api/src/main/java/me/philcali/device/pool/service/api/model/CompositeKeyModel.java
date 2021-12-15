package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

@ApiModel
@Value.Immutable
abstract class CompositeKeyModel {
    private static final String DELIMITER = ":";

    abstract String account();

    abstract String keyType();

    @Nullable
    abstract List<String> resources();

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner(DELIMITER)
                .add(account())
                .add(keyType());
        Optional.ofNullable(resources()).ifPresent(res -> res.forEach(joiner::add));
        return joiner.toString();
    }

    public static CompositeKey fromString(final String key) {
        String[] parts = key.split(DELIMITER);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Encoded key must contain an account and keyType, received: " + key);
        }
        CompositeKey.Builder builder = CompositeKey.builder()
                .account(parts[0])
                .keyType(parts[1]);
        Arrays.stream(parts).skip(2).forEach(builder::addResources);
        return builder.build();
    }
}
