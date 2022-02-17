/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.UpdateLockObject;
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

/**
 * <p>LockRepoDynamo class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Singleton
public class LockRepoDynamo
        extends AbstractObjectRepo<LockObject, CreateLockObject, UpdateLockObject>
        implements LockRepo {
    private static final String SINGLETON = "singleton";
    /** Constant <code>RESOURCE="lock"</code> */
    public static final String RESOURCE = "lock";

    @Inject
    /**
     * <p>Constructor for LockRepoDynamo.</p>
     *
     * @param table a {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable} object
     * @param marshaller a {@link me.philcali.device.pool.service.data.token.TokenMarshaller} object
     */
    public LockRepoDynamo(final DynamoDbTable<LockObject> table, final TokenMarshaller marshaller) {
        super(RESOURCE, table, marshaller);
    }

    /** {@inheritDoc} */
    @Override
    protected PutItemEnhancedRequest<LockObject> putItemRequest(
            CompositeKey account,
            CreateLockObject create) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return PutItemEnhancedRequest.builder(LockObject.class)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#id) and #name <> :id")
                        .putExpressionName("#id", PK)
                        .putExpressionName("#name", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(SINGLETON).build())
                        .build())
                .item(LockObject.builder()
                        .createdAt(now)
                        .updatedAt(now)
                        .id(SINGLETON)
                        .expiresIn(create.expiresIn())
                        .holder(create.holder())
                        .key(toPartitionKey(account))
                        .build())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    protected UpdateItemEnhancedRequest<LockObject> updateItemRequest(
            CompositeKey account,
            UpdateLockObject update) {
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return UpdateItemEnhancedRequest.builder(LockObject.class)
                .ignoreNulls(true)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(#id) and #name = :id and #holder = :holder")
                        .putExpressionName("#id", PK)
                        .putExpressionName("#name", SK)
                        .putExpressionName("#holder", "holder")
                        .putExpressionValue(":id", AttributeValue.builder().s(SINGLETON).build())
                        .putExpressionValue(":holder", AttributeValue.builder().s(update.holder()).build())
                        .build())
                .item(LockObject.builder()
                        .updatedAt(now)
                        .id(SINGLETON)
                        .holder(update.holder())
                        .key(toPartitionKey(account))
                        .expiresIn(update.expiresIn())
                        .build())
                .build();
    }
}
