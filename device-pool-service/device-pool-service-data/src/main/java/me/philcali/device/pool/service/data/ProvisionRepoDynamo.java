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
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ProvisionRepoDynamo
        extends AbstractObjectRepo<ProvisionObject, CreateProvisionObject, UpdateProvisionObject>
        implements ProvisionRepo {
    private static final String RESOURCE = "provision";

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
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(#key) and #id = :id")
                        .putExpressionName("#key", PK)
                        .putExpressionName("#id", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(update.id()).build())
                        .build())
                .build();
    }
}
