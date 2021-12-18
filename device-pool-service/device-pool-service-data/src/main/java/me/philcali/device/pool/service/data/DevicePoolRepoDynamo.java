package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.data.exception.TokenMarshallerException;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import javax.inject.Inject;
import java.util.Collections;
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
                            .partitionValue(toCompositeKey(compositeKey))
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
                    .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(toCompositeKey(key))))
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
                    .partitionValue(toCompositeKey(compositeKey))
                    .sortValue(poolId)
                    .build());
        } catch (DynamoDbException dbe) {
            LOGGER.error("Failed to delete device pool {} {}", compositeKey, poolId, dbe);
            throw new ServiceException(dbe);
        }
    }

    private String toCompositeKey(CompositeKey key) {
        return CompositeKey.builder()
                .from(key)
                .addResources(RESOURCE)
                .build()
                .toString();
    }
}
