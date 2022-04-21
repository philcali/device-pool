/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.health;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ColdStartTrigger implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColdStartTrigger.class);
    private final DevicePoolRepo pools;

    @Inject
    public ColdStartTrigger(DevicePoolRepoDynamo pools) {
        this.pools = pools;
    }

    @Override
    public void run() {
        try {
            pools.list(CompositeKey.of("012345678912"), QueryParams.builder()
                    .limit(1)
                    .build());
            LOGGER.info("Done");
        } catch (Exception e) {
            LOGGER.warn("Failed to list pools, but that's ok {}", e.getMessage());
        }
    }
}
