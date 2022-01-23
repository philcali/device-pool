/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Singleton
public class DeviceRepoDynamo
        extends AbstractObjectRepo<DeviceObject, CreateDeviceObject, UpdateDeviceObject>
        implements DeviceRepo {
    public static final String RESOURCE = "device";

    @Inject
    public DeviceRepoDynamo(
            final DynamoDbTable<DeviceObject> table,
            final TokenMarshaller marshaller) {
        super(RESOURCE, table, marshaller);
    }

    @Override
    protected PutItemEnhancedRequest<DeviceObject> putItemRequest(CompositeKey account, CreateDeviceObject create) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DeviceObject newDevice = DeviceObject.builder()
                .key(toPartitionKey(account))
                .createdAt(now)
                .updatedAt(now)
                .id(create.id())
                .privateAddress(create.privateAddress())
                .publicAddress(create.publicAddress())
                .expiresIn(create.expiresIn())
                .build();
        return PutItemEnhancedRequest.builder(DeviceObject.class)
                .item(newDevice)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#key) and #id <> :id")
                        .putExpressionName("#key", PK)
                        .putExpressionName("#id", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(create.id()).build())
                        .build())
                .build();
    }

    @Override
    protected UpdateItemEnhancedRequest<DeviceObject> updateItemRequest(
            CompositeKey account, UpdateDeviceObject update) {
        return UpdateItemEnhancedRequest.builder(DeviceObject.class)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(#key) and #id = :id")
                        .putExpressionName("#key", PK)
                        .putExpressionName("#id", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(update.id()).build())
                        .build())
                .ignoreNulls(true)
                .item(DeviceObject.builder()
                        .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                        .id(update.id())
                        .key(toPartitionKey(account))
                        .publicAddress(update.publicAddress())
                        .privateAddress(update.privateAddress())
                        .expiresIn(update.expiresIn())
                        .build())
                .build();
    }
}
