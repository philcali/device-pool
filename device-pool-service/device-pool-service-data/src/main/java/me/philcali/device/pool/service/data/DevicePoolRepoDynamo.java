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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

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
        try {
            PageIterable<DevicePoolObject> pages = table.query(QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(toPartitionValue(key))))
                    .exclusiveStartKey(marshaller.unmarshall(key, params.nextToken()))
                    .limit(params.limit())
                    .build());
            for (Page<DevicePoolObject> page : pages) {
                return QueryResults.<DevicePoolObject>builder()
                        .results(page.items())
                        .nextToken(marshaller.marshall(key, page.lastEvaluatedKey()))
                        .build();
            }
        } catch (TokenMarshallerException e) {
            LOGGER.error("Failed to marshal token for {}", key, e);
            throw new ServiceException(e);
        } catch (DynamoDbException dbe) {
            LOGGER.error("Failed to list device pools for {}", key, dbe);
            throw new ServiceException(dbe);
        }
        return QueryResults.<DevicePoolObject>builder()
                .results(Collections.emptyList())
                .build();
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
        UUID newId = UUID.randomUUID();
        Instant createdAt = Instant.ofEpochSecond(Instant.now().getEpochSecond());
        DevicePoolObject newObject = DevicePoolObject.builder()
                .id(newId.toString())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .name(create.name())
                .description(create.description())
                .account(toPartitionKey(compositeKey))
                .build();
        try {
            table.putItem(PutItemEnhancedRequest.builder(DevicePoolObject.class)
                    .item(newObject)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_not_exists(#id)")
                            .putExpressionName("#id", "PK")
                            .build())
                    .build());
            return newObject;
        } catch (ConditionalCheckFailedException e) {
            LOGGER.warn("Failed to create pool for {} with id {}, because it already exists", compositeKey, newId, e);
            throw new ConflictException("DevicePool with id " + newObject.id() + " already exists.");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to create pool for {} with id {}", compositeKey, newId, e);
            throw new ServiceException(e);
        }
    }

    @Override
    public DevicePoolObject update(CompositeKey compositeKey, UpdateDevicePoolObject update)
            throws NotFoundException, ServiceException {
        return null;
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
