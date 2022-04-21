/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ColdStartTrigger implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColdStartTrigger.class);
    private final DynamoDbClient client;

    @Inject
    public ColdStartTrigger(DynamoDbClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            client.listTables();
            LOGGER.info("Done");
        } catch (Exception e) {
            LOGGER.warn("Failed to list tables, but that's ok {}", e.getMessage());
        }
    }
}
