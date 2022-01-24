/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DeviceLockRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceLockObject;
import me.philcali.device.pool.service.api.model.DeviceLockObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceLockObject;
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
public class DeviceLockRepoDynamo
        extends AbstractObjectRepo<DeviceLockObject, CreateDeviceLockObject, UpdateDeviceLockObject>
        implements DeviceLockRepo {
    public static String RESOURCE = "lock";

    @Inject
    public DeviceLockRepoDynamo(DynamoDbTable<DeviceLockObject> table, TokenMarshaller marshaller) {
        super(RESOURCE, table, marshaller);
    }

    @Override
    protected PutItemEnhancedRequest<DeviceLockObject> putItemRequest(
            CompositeKey account,
            CreateDeviceLockObject create) {
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return PutItemEnhancedRequest.builder(DeviceLockObject.class)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#id) and #name <> :id")
                        .putExpressionName("#id", PK)
                        .putExpressionName("#name", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(create.id()).build())
                        .build())
                .item(DeviceLockObject.builder()
                        .createdAt(now)
                        .updatedAt(now)
                        .expiresIn(create.expiresIn())
                        .key(toPartitionKey(account))
                        .id(create.id())
                        .provisionId(create.provisionId())
                        .reservationId(create.reservationId())
                        .build())
                .build();
    }

    @Override
    protected UpdateItemEnhancedRequest<DeviceLockObject> updateItemRequest(
            CompositeKey account,
            UpdateDeviceLockObject update) {
        return UpdateItemEnhancedRequest.builder(DeviceLockObject.class)
                .ignoreNulls(true)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(#id) and #name = :id and #pid = :pid and #rid = :rid")
                        .putExpressionName("#id", PK)
                        .putExpressionName("#name", SK)
                        .putExpressionName("#pid", "provisionId")
                        .putExpressionName("#rid", "reservationId")
                        .putExpressionValue(":id", AttributeValue.builder().s(update.id()).build())
                        .putExpressionValue(":pid", AttributeValue.builder().s(update.provisionId()).build())
                        .putExpressionValue(":rid", AttributeValue.builder().s(update.reservationId()).build())
                        .build())
                .item(DeviceLockObject.builder()
                        .id(update.id())
                        .key(toPartitionKey(account))
                        .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                        .expiresIn(update.expiresIn())
                        .reservationId(update.reservationId())
                        .provisionId(update.provisionId())
                        .build())
                .build();
    }
}
