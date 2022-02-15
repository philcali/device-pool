/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.lock;

import me.philcali.device.pool.exceptions.LockingException;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;

import java.util.concurrent.CompletableFuture;

/**
 * A component to facilitate locking, ideally through a distributed system, but
 * the contract does not enforce it. The attempt to lock on an arbitrary input
 * in the form of {@link LockInput}, will block according to client control via
 * a {@link CompletableFuture}: indefinitely or timeout. Once a lock is in control
 * of, via {@link LockOutput}, the client can extend or release held locks. Ideally,
 * locks automatically expire, but implementations may decide not to.
 */
public interface LockingMechanism {
    /**
     * Attempts to acquire a lock on the unique identifier, which is included as the id
     * of the {@link LockInput}.
     *
     * @param input The lock information for the {@link LockingMechanism} to hold on to
     * @return A {@link CompletableFuture} that results in a {@link LockOutput} when successfully acquires a lock
     */
    CompletableFuture<LockOutput> lock(LockInput input);

    /**
     * Attempts to extend the lock that is in the client control. An attempt to extend a lock that is
     * not in the client control will result in a conflict, surfaced as a
     * {@link me.philcali.device.pool.exceptions.LockingConflictException}
     *
     * @param input The lock information held by the client in the form of a {@link LockInput}
     * @return The updated lock metadata in the form of a {@link LockOutput}
     * @throws LockingException Failure to extend the lock
     */
    LockOutput extend(LockInput input) throws LockingException;

    /**
     * Forcibly releases the lock that may or may not be held by the client.
     *
     * @param lockId The unique lock identifier to release
     * @throws LockingException Failure to release the lock being held
     */
    void lease(String lockId) throws LockingException;
}
