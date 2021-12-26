package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
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
import java.util.Objects;
import java.util.Optional;

@Singleton
public class DevicePoolRepoDynamo
        extends AbstractObjectRepo<DevicePoolObject, CreateDevicePoolObject, UpdateDevicePoolObject>
        implements DevicePoolRepo {
    public static final String RESOURCE = "pool";

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
                .endpoint(create.endpoint())
                .type(create.type())
                .key(toPartitionKey(account))
                .build();
        if (Objects.isNull(create.endpoint()) && create.type() == DevicePoolType.UNMANAGED) {
            throw new InvalidInputException("Cannot have an empty endpoint for an " + create.type() + " pool");
        }
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
        final Expression.Builder builder = Expression.builder()
                .putExpressionName("#id", PK)
                .putExpressionName("#name", SK)
                .putExpressionValue(":id", AttributeValue.builder()
                        .s(update.name())
                        .build());
        final StringBuilder expression = new StringBuilder()
                .append("attribute_exists(#id) and #name = :id");
        Optional.ofNullable(update.type()).ifPresent(type -> {
            expression.append(" and #type = :type");
            builder.putExpressionName("#type", "type")
                    .putExpressionValue(":type", AttributeValue.builder()
                            .s(type.name())
                            .build());
        });
        builder.expression(expression.toString());
        return UpdateItemEnhancedRequest.builder(DevicePoolObject.class)
                .ignoreNulls(true)
                .conditionExpression(builder.build())
                .item(DevicePoolObject.builder()
                        .name(update.name())
                        .description(update.description())
                        .endpoint(update.endpoint())
                        .type(update.type())
                        .key(toPartitionKey(account))
                        .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                        .build())
                .build();
    }
}
