/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.lock;

import me.philcali.device.pool.exceptions.LockingException;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface LockingMechanism {
    CompletableFuture<LockOutput> lock(LockInput input);

    LockOutput extend(LockInput input) throws LockingException;

    void lease(String lockId) throws LockingException;
}
