package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
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
public class DevicePoolRepoDynamo
        extends AbstractObjectRepo<DevicePoolObject, CreateDevicePoolObject, UpdateDevicePoolObject>
        implements DevicePoolRepo {
    private static final String RESOURCE = "pool";

    @Inject
    public DevicePoolRepoDynamo(
            DynamoDbTable<DevicePoolObject> table,
            TokenMarshaller marshaller) {
        super(RESOURCE, table, marshaller);
    }

    @Override
    protected PutItemEnhancedRequest<DevicePoolObject> putItemRequest(
            CompositeKey account, CreateDevicePoolObject create) {
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DevicePoolObject newObject = DevicePoolObject.builder()
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .name(create.name())
                .description(create.description())
                .key(toPartitionKey(account))
                .build();
        return PutItemEnhancedRequest.builder(DevicePoolObject.class)
                .item(newObject)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#id) and #name <> :id")
                        .putExpressionName("#id", PK)
                        .putExpressionName("#name", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(create.name()).build())
                        .build())
                .build();
    }

    @Override
    protected UpdateItemEnhancedRequest<DevicePoolObject> updateItemRequest(
            CompositeKey account, UpdateDevicePoolObject update) {
        return UpdateItemEnhancedRequest.builder(DevicePoolObject.class)
                .ignoreNulls(true)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(#id) and #name = :id")
                        .putExpressionName("#id", PK)
                        .putExpressionName("#name", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(update.name()).build())
                        .build())
                .item(DevicePoolObject.builder()
                        .name(update.name())
                        .description(update.description())
                        .key(toPartitionKey(account))
                        .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                        .build())
                .build();
    }
}
