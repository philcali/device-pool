/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.model;

import java.time.Duration;

public interface LockingConfiguration {
    boolean locking();

    Duration lockingDuration();
}
