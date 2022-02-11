/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ProvisionRepoDynamo
        extends AbstractObjectRepo<ProvisionObject, CreateProvisionObject, UpdateProvisionObject>
        implements ProvisionRepo {
    public static final String RESOURCE = "provision";

    @Inject
    public ProvisionRepoDynamo(final DynamoDbTable<ProvisionObject> table, final TokenMarshaller marshaller) {
        super(RESOURCE, table, marshaller);
    }

    @Override
    protected PutItemEnhancedRequest<ProvisionObject> putItemRequest(
            CompositeKey account, CreateProvisionObject create) {
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return PutItemEnhancedRequest.builder(ProvisionObject.class)
                .item(ProvisionObject.builder()
                        .id(create.id())
                        .key(toPartitionKey(account))
                        .amount(create.amount())
                        .createdAt(now)
                        .updatedAt(now)
                        .expiresIn(create.expiresIn())
                        .status(Status.REQUESTED)
                        .build())
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#key) and #id <> :id")
                        .putExpressionName("#key", PK)
                        .putExpressionName("#id", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(create.id()).build())
                        .build())
                .build();
    }

    @Override
    protected UpdateItemEnhancedRequest<ProvisionObject> updateItemRequest(
            CompositeKey account, UpdateProvisionObject update) {
        return UpdateItemEnhancedRequest.builder(ProvisionObject.class)
                .ignoreNulls(true)
                .item(ProvisionObject.builder()
                        .id(update.id())
                        .key(toPartitionKey(account))
                        .expiresIn(update.expiresIn())
                        .status(update.status())
                        .message(update.message())
                        .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                        .build())
                // Can't transition from a terminal to a non-terminal status
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(#key) and #id = :id")
                        .putExpressionName("#key", PK)
                        .putExpressionName("#id", SK)
                        .putExpressionValue(":id", AttributeValue.builder()
                                .s(update.id())
                                .build())
                        .build())
                .build();
    }

    @Override
    protected DeleteItemEnhancedRequest.Builder deleteRequest(Key key) {
        List<String> statusValueKeys = new ArrayList<>();
        Expression.Builder builder = Expression.builder();
        int index = 0;
        for (Status status : Status.values()) {
            // We allow an exception fo cancelling, since we need to seed the stream
            if (status.isTerminal() || status == Status.CANCELING) {
                String valueKey = ":s" + (++index);
                statusValueKeys.add(valueKey);
                builder.putExpressionValue(valueKey, AttributeValue.builder()
                        .s(status.name())
                        .build());
            }
        }
        return super.deleteRequest(key).conditionExpression(builder
                .expression("#status in (" + String.join(", ", statusValueKeys) + ")")
                .putExpressionName("#status", "status")
                .build());
    }
}
