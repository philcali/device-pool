/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.lock;

import me.philcali.device.pool.exceptions.LockingConflictException;
import me.philcali.device.pool.exceptions.LockingException;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link LockingMechanism} that is entirely local and in-memory. It wraps a
 * {@link ConcurrentHashMap} for held locks. The purpose of this {@link LockingMechanism}
 * is to facilitate integration for using the mechanism in lock decorations like the
 * {@link LockingService} or {@link me.philcali.device.pool.provision.LockingProvisionService}
 */
public class LocalLockingMechanism implements LockingMechanism {
    private final Map<String, CacheValue> activeLocks = new ConcurrentHashMap<>();

    private static class CacheValue {
        String holder;
        LockOutput lock;
    }

    @Override
    public CompletableFuture<LockOutput> lock(LockInput input) {
        return CompletableFuture.supplyAsync(() -> {
            for (;;) {
                try {
                    return extend(input);
                } catch (LockingConflictException e) {
                    // no-op, purpose of this future is to try indefinitely
                }
            }
        });
    }

    @Override
    public void lease(String lockId) throws LockingException {
        activeLocks.remove(lockId);
    }

    @Override
    public LockOutput extend(LockInput input) throws LockingException {
        return activeLocks.compute(input.id(), (id, cache) -> {
            Instant now = Instant.now();
            if (cache == null || cache.lock.expiresIn() * 1000 <= System.currentTimeMillis()) {
                CacheValue newCacheValue = new CacheValue();
                newCacheValue.lock = LockOutput.builder()
                        .id(input.id())
                        .value(input.value())
                        .expiresIn(now.plus(input.ttl(), ChronoUnit.SECONDS).getEpochSecond())
                        .updatedAt(now.getEpochSecond())
                        .build();
                newCacheValue.holder = input.holder();
                return newCacheValue;
            } else if (cache.holder.equals(input.holder())) {
                cache.lock = LockOutput.builder()
                        .from(cache.lock)
                        .updatedAt(now.getEpochSecond())
                        .expiresIn(now.plus(input.ttl(), ChronoUnit.SECONDS).getEpochSecond())
                        .build();
                return cache;
            } else {
                throw new LockingConflictException("Cache value " + input.id() + " is being held");
            }
        }).lock;
    }
}
