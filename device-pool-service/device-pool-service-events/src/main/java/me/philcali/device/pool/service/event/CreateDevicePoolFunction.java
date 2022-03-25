/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Map;

/**
 * This handler exists for the SDK construct which supports only direct DDB calls for inserting
 * {@link me.philcali.device.pool.service.api.model.DevicePoolObject} resources. This is to by pass
 * a limitation where the timestamp must exist, but not reflect in an altogether new resource.
 */
@Singleton
public class CreateDevicePoolFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDevicePoolFunction.class);
    private final DynamoDbClient ddb;
    private final DynamoDbTable<DevicePoolObject> table;

    @Inject
    public CreateDevicePoolFunction(final DynamoDbClient ddb, final DynamoDbTable<DevicePoolObject> table) {
        this.ddb = ddb;
        this.table = table;
    }

    @Override
    public boolean test(Record record) {
        return record.getEventName().equals(OperationType.INSERT.toString())
                && primaryKey(record).endsWith(DevicePoolRepoDynamo.RESOURCE)
                && !record.getDynamodb().getNewImage().containsKey("createdAt");
    }

    @Override
    public void accept(Map<String, AttributeValue> newImage, Map<String, AttributeValue> oldImage) {
        long now = Instant.now().getEpochSecond();
        try {
            ddb.updateItem(UpdateItemRequest.builder()
                    .key(Map.of(PK, newImage.get(PK), SK, newImage.get(SK)))
                    .tableName(table.tableName())
                    .updateExpression("SET #createdAt = :now, #updatedAt = :now")
                    .expressionAttributeNames(Map.of(
                            "#pk", PK,
                            "#sk", SK,
                            "#createdAt", "createdAt",
                            "#updatedAt", "updatedAt"))
                    .expressionAttributeValues(Map.of(
                            ":name", newImage.get(SK),
                            ":now", AttributeValue.builder().n(Long.toString(now)).build()))
                    .conditionExpression("attribute_exists(#pk) and #sk = :name")
                    .build());
        } catch (ConditionalCheckFailedException cfe) {
            LOGGER.warn("Device pool for {} no longer exists", newImage.get(SK).s());
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to set the creation / update timestamp for {}", newImage.get(SK).s(), e);
        }
    }
}
