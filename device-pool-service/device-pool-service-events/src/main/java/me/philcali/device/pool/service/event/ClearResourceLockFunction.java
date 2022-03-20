/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.data.LockRepoDynamo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class ClearResourceLockFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearResourceLockFunction.class);
    private final LockRepo lockRepo;

    @Inject
    public ClearResourceLockFunction(final LockRepo lockRepo) {
        this.lockRepo = lockRepo;
    }

    @Override
    public boolean test(Record record) {
        // Catch any resource delete that is not the lock resource
        return record.getEventName().equals(OperationType.REMOVE.name())
                && !primaryKey(record).endsWith(LockRepoDynamo.RESOURCE);
    }

    @Override
    public void accept(Map<String, AttributeValue> newImage, Map<String, AttributeValue> oldImage) {
        CompositeKey resourceKey = CompositeKey.builder()
                .from(CompositeKey.fromString(oldImage.get(PK).s()))
                .addResources(oldImage.get(SK).s())
                .build();
        LOGGER.info("Clearing any locks for {}", resourceKey);
        lockRepo.delete(resourceKey, LockRepo.SINGLETON);
        LOGGER.info("Removed held lock for {}", resourceKey);
    }
}
