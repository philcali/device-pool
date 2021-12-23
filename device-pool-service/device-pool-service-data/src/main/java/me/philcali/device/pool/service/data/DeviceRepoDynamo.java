package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
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

public class DeviceRepoDynamo implements DeviceRepo {
    private static final String RESOURCE = "Devices";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRepoDynamo.class);

    private final DynamoDbTable<DeviceObject> table;
    private final TokenMarshaller marshaller;

    @Inject
    public DeviceRepoDynamo(
            final DynamoDbTable<DeviceObject> table,
            final TokenMarshaller marshaller) {
        this.table = table;
        this.marshaller = marshaller;
    }

    @Override
    public DeviceObject get(CompositeKey account, String id) throws NotFoundException, ServiceException {
        try {
            return Optional.ofNullable(table.getItem(Key.builder()
                    .partitionValue(toPartitionString(account))
                    .sortValue(id)
                    .build()))
                    .orElseThrow(() -> new NotFoundException("Could not find a device for " + id));
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to get a device {}: {}", account, id, e);
            throw new ServiceException(e);
        }
    }

    @Override
    public QueryResults<DeviceObject> list(CompositeKey account, QueryParams params) throws ServiceException {
        CompositeKey owner = toPartitionKey(account);
        try {
            PageIterable<DeviceObject> pages = table.query(QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(owner.toString())))
                    .limit(params.limit())
                    .exclusiveStartKey(marshaller.unmarshall(owner, params.nextToken()))
                    .build());
            QueryResults.Builder<DeviceObject> devices = QueryResults.builder();
            for (Page<DeviceObject> page : pages) {
                devices.nextToken(marshaller.marshall(owner, page.lastEvaluatedKey()))
                        .addAllResults(page.items());
                break;
            }
            return devices.build();
        } catch (TokenMarshallerException e) {
            LOGGER.error("Failed to marshall token {} for {}", params.nextToken(), owner, e);
            throw new ServiceException(e);
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to list devices for {}", owner, e);
            throw new ServiceException(e);
        }
    }

    @Override
    public DeviceObject create(CompositeKey account, CreateDeviceObject create)
            throws ConflictException, ServiceException {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DeviceObject newDevice = DeviceObject.builder()
                .key(toPartitionKey(account))
                .createdAt(now)
                .updatedAt(now)
                .id(create.id())
                .privateAddress(create.privateAddress())
                .publicAddress(create.publicAddress())
                .expiresIn(create.expiresIn())
                .build();
        try {
            table.putItem(PutItemEnhancedRequest.builder(DeviceObject.class)
                    .item(newDevice)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_not_exists(#key) and #id <> :id")
                            .putExpressionName("#key", "PK")
                            .putExpressionName("#id", "SK")
                            .putExpressionValue(":id", AttributeValue.builder().s(create.id()).build())
                            .build())
                    .build());
            return newDevice;
        } catch (ConditionalCheckFailedException e) {
            LOGGER.debug("Device {} already exists for {}", create.id(), account);
            throw new ConflictException("Device " + create.id() + " already exists.");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to create a device for {}: {}", account, create.id(), e);
            throw new ServiceException(e);
        }
    }

    @Override
    public DeviceObject update(CompositeKey account, UpdateDeviceObject update)
            throws NotFoundException, ServiceException {
        try {
            return table.updateItem(UpdateItemEnhancedRequest.builder(DeviceObject.class)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_exists(#key) and #id = :id")
                            .putExpressionName("#key", "PK")
                            .putExpressionName("#id", "SK")
                            .putExpressionValue(":id", AttributeValue.builder().s(update.id()).build())
                            .build())
                    .ignoreNulls(true)
                    .item(DeviceObject.builder()
                            .updatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                            .id(update.id())
                            .key(toPartitionKey(account))
                            .publicAddress(update.publicAddress())
                            .privateAddress(update.privateAddress())
                            .expiresIn(update.expiresIn())
                            .build())
                    .build());
        } catch (ConditionalCheckFailedException | ResourceNotFoundException e) {
            LOGGER.debug("Device {} is not found for {}", update.id(), account, e);
            throw new NotFoundException("Device " + update.id() + " is not found.");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to update device for {}: {}", account, update.id(), e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void delete(CompositeKey account, String id) throws ServiceException {
        try {
            table.deleteItem(Key.builder()
                    .partitionValue(toPartitionString(account))
                    .sortValue(id)
                    .build());
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to delete device {}: {}", account, id, e);
            throw new ServiceException(e);
        }
    }

    private CompositeKey toPartitionKey(CompositeKey account) {
        return CompositeKey.builder()
                .from(account)
                .addResources(RESOURCE)
                .build();
    }

    private String toPartitionString(CompositeKey account) {
        return toPartitionKey(account).toString();
    }
}
