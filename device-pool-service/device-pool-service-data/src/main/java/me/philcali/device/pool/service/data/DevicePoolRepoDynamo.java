package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
import me.philcali.device.pool.service.data.exception.TokenMarshallerException;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class DevicePoolRepoDynamo implements DevicePoolRepo {
    private static final String RESOURCE = "DevicePool";
    private static final Logger LOGGER = LoggerFactory.getLogger(DevicePoolRepoDynamo.class);
    private final DynamoDbTable<DevicePoolObject> table;
    private final TokenMarshaller marshaller;

    @Inject
    public DevicePoolRepoDynamo(
            DynamoDbTable<DevicePoolObject> table,
            TokenMarshaller marshaller) {
        this.marshaller = marshaller;
        this.table = table;
    }

    @Override
    public DevicePoolObject get(CompositeKey compositeKey, String poolId) throws NotFoundException {
        try {
            return Optional.ofNullable(table.getItem(Key.builder()
                            .partitionValue(toPartitionValue(compositeKey))
                            .sortValue(poolId)
                            .build()))
                    .orElseThrow(() -> new NotFoundException("Could not find a device pool with id: " + poolId));
        } catch (DynamoDbException dbe) {
            LOGGER.error("Failed to get device pool with poolId: {} - {}", compositeKey, poolId, dbe);
            throw new ServiceException(dbe);
        }
    }

    @Override
    public QueryResults<DevicePoolObject> list(CompositeKey key, QueryParams params) {
        QueryResults.Builder<DevicePoolObject> builder = QueryResults.builder();
        try {
            CompositeKey pagingAccount = toPartitionKey(key);
            PageIterable<DevicePoolObject> pages = table.query(QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(pagingAccount.toString())))
                    .exclusiveStartKey(marshaller.unmarshall(pagingAccount, params.nextToken()))
                    .limit(params.limit())
                    .build());
            for (Page<DevicePoolObject> page : pages) {
                builder.addAllResults(page.items())
                        .nextToken(marshaller.marshall(pagingAccount, page.lastEvaluatedKey()));
                break;
            }
        } catch (TokenMarshallerException e) {
            LOGGER.error("Failed to marshal token for {}", key, e);
            throw new ServiceException(e);
        } catch (DynamoDbException dbe) {
            LOGGER.error("Failed to list device pools for {}", key, dbe);
            throw new ServiceException(dbe);
        }
        return builder.build();
    }

    @Override
    public void delete(CompositeKey compositeKey, String poolId) {
        try {
            table.deleteItem(Key.builder()
                    .partitionValue(toPartitionValue(compositeKey))
                    .sortValue(poolId)
                    .build());
        } catch (DynamoDbException dbe) {
            LOGGER.error("Failed to delete device pool {} {}", compositeKey, poolId, dbe);
            throw new ServiceException(dbe);
        }
    }

    @Override
    public DevicePoolObject create(CompositeKey compositeKey, CreateDevicePoolObject create)
            throws ConflictException, ServiceException {
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DevicePoolObject newObject = DevicePoolObject.builder()
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .name(create.name())
                .description(create.description())
                .key(toPartitionKey(compositeKey))
                .build();
        try {
            table.putItem(PutItemEnhancedRequest.builder(DevicePoolObject.class)
                    .item(newObject)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_not_exists(#id) and #name <> :name")
                            .putExpressionName("#id", "PK")
                            .putExpressionName("#name", "SK")
                            .putExpressionValue(":name", AttributeValue.builder().s(create.name()).build())
                            .build())
                    .build());
            return newObject;
        } catch (ConditionalCheckFailedException e) {
            LOGGER.warn("Failed to create pool for {} with name {}, because it already exists",
                    compositeKey, create.name(), e);
            throw new ConflictException("DevicePool with name " + newObject.name() + " already exists.");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to create pool for {} with name {}", compositeKey, create.name(), e);
            throw new ServiceException(e);
        }
    }

    @Override
    public DevicePoolObject update(CompositeKey compositeKey, UpdateDevicePoolObject update)
            throws NotFoundException, ServiceException {
        try {
            Instant updateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            return table.updateItem(UpdateItemEnhancedRequest.builder(DevicePoolObject.class)
                    .ignoreNulls(true)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_exists(#id) and #name = :name")
                            .putExpressionName("#id", "PK")
                            .putExpressionName("#name", "SK")
                            .putExpressionValue(":name", AttributeValue.builder().s(update.name()).build())
                            .build())
                    .item(DevicePoolObject.builder()
                            .name(update.name())
                            .description(update.description())
                            .key(toPartitionKey(compositeKey))
                            .updatedAt(updateTime)
                            .build())
                    .build());
        } catch (ConditionalCheckFailedException | ResourceNotFoundException e) {
            LOGGER.warn("Failed check on update for {} - {}", compositeKey, update.name());
            throw new NotFoundException("DevicePool with name " + update.name() + " was not found");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to update pool named {} for {} ", update.name(), compositeKey, e);
            throw new ServiceException(e);
        }
    }

    private CompositeKey toPartitionKey(CompositeKey key) {
        return CompositeKey.builder()
                .from(key)
                .addResources(RESOURCE)
                .build();
    }

    private String toPartitionValue(CompositeKey key) {
        return toPartitionKey(key).toString();
    }
}
