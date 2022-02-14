/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ddb;

import me.philcali.device.pool.exceptions.LockingConflictException;
import me.philcali.device.pool.exceptions.LockingException;
import me.philcali.device.pool.lock.LockingMechanism;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@ApiModel
@Value.Immutable
abstract class LockingMechanismDynamoDBModel implements LockingMechanism {
    private static final Logger LOGGER = LogManager.getLogger(LockingMechanismDynamoDB.class);
    private static final String ID = "id";
    private static final String HOLDER = "holder";
    private static final String VALUE = "value";
    private static final String EXPIRES_IN = "expiresIn";
    private static final String UPDATED_AT = "updatedAt";

    @Value.Default
    DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.create();
    }

    @Value.Default
    long pulseTime() {
        return 100;
    }

    abstract String tableName();

    @Value.Default
    ExecutorService executorService() {
        return Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("DynamoDB-Locking-Thread");
            return thread;
        });
    }

    private LockOutput putLock(final LockInput input) {
        final long now = Instant.now().getEpochSecond();
        final long expiresIn = now + input.ttl();
        final String expression = "attribute_not_exists(#id) OR ("
                + " (attribute_exists(#id) AND #expiresIn > :now) AND"
                + " (attribute_exists(#id) AND #holder = :holderId))";
        final PutItemResponse putItemResponse = dynamoDbClient().putItem(PutItemRequest.builder()
                .tableName(tableName())
                .returnValues(ReturnValue.ALL_OLD)
                .conditionExpression(expression)
                .expressionAttributeNames(new HashMap<String, String>() {{
                    put("#id", ID);
                    put("#expiresIn", EXPIRES_IN);
                    put("#holder", HOLDER);
                }})
                .expressionAttributeValues(new HashMap<String, AttributeValue>() {{
                    put(":now", AttributeValue.builder().n(Long.toString(now)).build());
                    put(":holderId", AttributeValue.builder().s(input.holder()).build());
                }})
                .item(new HashMap<String, AttributeValue>() {{
                    put(ID, AttributeValue.builder().s(input.id()).build());
                    put(HOLDER, AttributeValue.builder().s(input.holder()).build());
                    if (input.value() != null) {
                        put(VALUE, AttributeValue.builder().s(input.value()).build());
                    }
                    put(UPDATED_AT, AttributeValue.builder().n(Long.toString(now)).build());
                    put(EXPIRES_IN, AttributeValue.builder().n(Long.toString(expiresIn)).build());
                }})
                .build());
        LockOutput output = LockOutput.builder()
                .id(input.id())
                .value(input.value())
                .expiresIn(expiresIn)
                .updatedAt(now)
                .build();
        LOGGER.debug("Obtained a new lock: {}, put response {}", output, putItemResponse.attributes());
        return output;
    }

    private Supplier<LockOutput> internalLock(final LockInput input) {
        return () -> {
            for (;;) {
                try {
                    return putLock(input);
                } catch (ConditionalCheckFailedException conditionalCheckFailedException) {
                    try {
                        Thread.sleep(pulseTime());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to obtain a lock: {}", input.id(), e);
                    throw new LockingException(e);
                }
            }
        };
    }

    @Override
    public CompletableFuture<LockOutput> lock(final LockInput input) {
        return CompletableFuture.supplyAsync(internalLock(input), executorService());
    }

    @Override
    public LockOutput extend(final LockInput input) {
        try {
            return putLock(input);
        } catch (ConditionalCheckFailedException e) {
            throw new LockingConflictException(e);
        } catch (DynamoDbException e) {
            throw new LockingException(e);
        }
    }

    @Override
    public void lease(final String lockId) throws LockingException {
        try {
            dynamoDbClient().deleteItem(DeleteItemRequest.builder()
                    .tableName(tableName())
                    .key(new HashMap<>() {{
                        put(ID, AttributeValue.builder().s(lockId).build());
                    }})
                    .build());
        } catch (DynamoDbException e) {
            throw new LockingException(e);
        }
    }
}
