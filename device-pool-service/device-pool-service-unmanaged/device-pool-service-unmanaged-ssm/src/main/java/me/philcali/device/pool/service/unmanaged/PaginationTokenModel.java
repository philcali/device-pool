/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;

import javax.annotation.Nullable;
import java.util.Optional;

@ApiModel
@Value.Immutable
abstract class PaginationTokenModel {
    private static final String DELIMITER = ":";
    public static final int MAX_ITEMS = 5;

    @Nullable
    abstract String nextToken();

    @Value.Default
    int index() {
        return 0;
    }

    public static PaginationToken create() {
        return PaginationToken.builder().build();
    }

    public PaginationToken nextPage(String nextToken) {
        int page = index() + 1;
        if (page >= MAX_ITEMS) {
            return PaginationToken.builder()
                    .nextToken(nextToken)
                    .build();
        } else {
            return PaginationToken.builder()
                    .from(this)
                    .index(page)
                    .build();
        }
    }

    public static PaginationToken fromString(String startingToken) {
        String[] parts = startingToken.split(":");
        return PaginationToken.builder()
                .nextToken(parts[0].isEmpty() ? null : parts[0])
                .index(Integer.parseInt(parts[1]))
                .build();
    }

    @Override
    public String toString() {
        return Optional.ofNullable(nextToken()).orElse("") + DELIMITER + index();
    }
}
