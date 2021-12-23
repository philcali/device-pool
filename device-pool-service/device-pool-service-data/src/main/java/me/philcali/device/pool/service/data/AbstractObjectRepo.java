package me.philcali.device.pool.service.data;

import me.philcali.device.pool.service.api.ObjectRepository;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
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
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.Optional;

abstract class AbstractObjectRepo<T, C, U> implements ObjectRepository<T, C, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractObjectRepo.class);
    private final String resourceName;
    private final DynamoDbTable<T> table;
    private final TokenMarshaller marshaller;

    AbstractObjectRepo(
            final String resourceName,
            final DynamoDbTable<T> table,
            final TokenMarshaller marshaller) {
        this.resourceName = resourceName;
        this.marshaller = marshaller;
        this.table = table;
    }

    protected CompositeKey toPartitionKey(CompositeKey account) {
        return CompositeKey.builder().from(account).addResources(resourceName).build();
    }

    protected  String toPartitionValue(CompositeKey account) {
        return toPartitionKey(account).toString();
    }

    @Override
    public T get(CompositeKey account, String id) throws NotFoundException, ServiceException {
        try {
            return Optional.ofNullable(table.getItem(Key.builder()
                            .partitionValue(toPartitionValue(account))
                            .sortValue(id)
                            .build()))
                    .orElseThrow(() -> new NotFoundException("Could not find a " + resourceName + " with id: " + id));
        } catch (DynamoDbException dbe) {
            LOGGER.error("Failed to get {} with id: {} - {}", resourceName, account, id, dbe);
            throw new ServiceException(dbe);
        }
    }

    @Override
    public void delete(CompositeKey account, String id) throws ServiceException {
        try {
            table.deleteItem(Key.builder()
                    .partitionValue(toPartitionValue(account))
                    .sortValue(id)
                    .build());
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to delete {} {}: {}", resourceName, account, id, e);
            throw new ServiceException(e);
        }
    }

    @Override
    public QueryResults<T> list(CompositeKey account, QueryParams params) throws ServiceException {
        CompositeKey owner = toPartitionKey(account);
        try {
            PageIterable<T> pages = table.query(QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(owner.toString())))
                    .limit(params.limit())
                    .exclusiveStartKey(marshaller.unmarshall(owner, params.nextToken()))
                    .build());
            QueryResults.Builder<T> results = QueryResults.builder();
            for (Page<T> page : pages) {
                results.nextToken(marshaller.marshall(owner, page.lastEvaluatedKey()))
                        .addAllResults(page.items());
                break;
            }
            return results.build();
        } catch (TokenMarshallerException e) {
            LOGGER.error("Failed to marshall token {} for {}", params.nextToken(), owner, e);
            throw new ServiceException(e);
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to list {} for {}", resourceName, owner, e);
            throw new ServiceException(e);
        }
    }

    protected abstract PutItemEnhancedRequest<T> putItemRequest(CompositeKey account, C create);

    @Override
    public T create(CompositeKey account, C create) throws ConflictException, ServiceException {
        PutItemEnhancedRequest<T> putItem = putItemRequest(account, create);
        try {
            table.putItem(putItem);
            return putItem.item();
        } catch (ConditionalCheckFailedException e) {
            LOGGER.debug("The {} {} already exists for {}", resourceName, create, account);
            throw new ConflictException("The" + resourceName + " " + putItem
                    .conditionExpression()
                    .expressionValues()
                    .get(":id") + " already exists.");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to create a {} for {}: {}", resourceName, account, create, e);
            throw new ServiceException(e);
        }
    }

    protected abstract UpdateItemEnhancedRequest<T> updateItemRequest(CompositeKey account, U update);

    @Override
    public T update(CompositeKey account, U update) throws NotFoundException, ServiceException {
        UpdateItemEnhancedRequest<T> request = updateItemRequest(account, update);
        try {
            return table.updateItem(request);
        } catch (ConditionalCheckFailedException | ResourceNotFoundException e) {
            LOGGER.debug("The {} {} is not found for {}", resourceName, update, account, e);
            throw new NotFoundException("The " + resourceName + " " + request.conditionExpression()
                    .expressionValues().get(":id").s() + " is not found.");
        } catch (DynamoDbException e) {
            LOGGER.error("Failed to update {} for {}: {}", resourceName, account, update, e);
            throw new ServiceException(e);
        }
    }
}
