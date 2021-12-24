package me.philcali.device.pool.service.data;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
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
public class ReservationRepoDynamo
        extends AbstractObjectRepo<ReservationObject, CreateReservationObject, UpdateReservationObject>
        implements ReservationRepo {
    private static final String RESOURCE = "reservation";

    @Inject
    public ReservationRepoDynamo(
            final DynamoDbTable<ReservationObject> table,
            final TokenMarshaller marshaller) {
        super(RESOURCE, table, marshaller);
    }

    @Override
    protected PutItemEnhancedRequest<ReservationObject> putItemRequest(
            CompositeKey account,
            CreateReservationObject create) {
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return PutItemEnhancedRequest.builder(ReservationObject.class)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#key) and #id <> :id")
                        .putExpressionName("#key", PK)
                        .putExpressionName("#id", SK)
                        .putExpressionValue(":id", AttributeValue.builder().s(create.id()).build())
                        .build())
                .item(ReservationObject.builder()
                        .id(create.id())
                        .key(toPartitionKey(account))
                        .deviceId(create.deviceId())
                        .createdAt(now)
                        .updatedAt(now)
                        .status(Status.REQUESTED)
                        .build())
                .build();
    }

    @Override
    protected UpdateItemEnhancedRequest<ReservationObject> updateItemRequest(
            CompositeKey account,
            UpdateReservationObject update) {
        return UpdateItemEnhancedRequest.builder(ReservationObject.class)
                .ignoreNulls(true)
                .item(ReservationObject.builder()
                        .id(update.id())
                        .key(toPartitionKey(account))
                        .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                        .status(update.status())
                        .message(update.message())
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
