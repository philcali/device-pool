package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
import me.philcali.device.pool.service.data.exception.TokenMarshallerException;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({DynamoDBExtension.class})
class DevicePoolRepoDynamoTest {
    private DevicePoolRepo poolRepo;
    static DynamoDbTable<DevicePoolObject> objectTable;

    @BeforeAll
    static void beforeAll(DynamoDbClient ddb) {
        DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        objectTable = client.table("DevicePool", TableSchemas.poolTableSchema());
        objectTable.createTable();
    }

    Set<String> createPools(CompositeKey account, int amount, String prefix) {
        Set<String> poolNames = new HashSet<>();
        IntStream.range(0, amount).forEach(index -> assertTrue(poolNames.add(poolRepo.create(account, CreateDevicePoolObject.builder()
                        .name(prefix + index)
                        .description("This pool is for device index " + index)
                        .build())
                .name())));
        return poolNames;
    }

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        ObjectMapper mapper = new ObjectMapper();
        EncryptedTokenMarshaller underlyingMarshaller = new EncryptedTokenMarshaller(mapper);
        poolRepo = new DevicePoolRepoDynamo(objectTable, underlyingMarshaller);
    }

    @Test
    void GIVEN_pool_is_created_WHEN_management_workflow_is_exercised_THEN_create_read_update_delete_contract() {
        CompositeKey key = CompositeKey.of("abc123456789");

        DevicePoolObject devicePool = poolRepo.create(key, CreateDevicePoolObject.builder()
                .name("TestDevicePool")
                .type(DevicePoolType.UNMANAGED)
                .endpoint(DevicePoolEndpoint.builder()
                        .type(DevicePoolEndpointType.HTTP)
                        .uri("http://example.com")
                        .build())
                .build());

        // Can't recreate an existing pool
        assertThrows(ConflictException.class, () -> poolRepo.create(key, c -> c.name(devicePool.name())));
        // Can't have an unmanaged pool and no endpoint
        assertThrows(InvalidInputException.class, () -> poolRepo.create(key, c -> c.name("DevicePoolUnmanaged")
                .type(DevicePoolType.UNMANAGED)));

        assertEquals(key.account(), devicePool.key().account());
        assertEquals("TestDevicePool", devicePool.name());

        DevicePoolObject obtainedObject = poolRepo.get(key, devicePool.name());
        assertEquals(devicePool, obtainedObject);

        DevicePoolObject updatedPool = poolRepo.update(key, UpdateDevicePoolObject.builder()
                .name(devicePool.name())
                .description("This is the description")
                .build());
        assertEquals(devicePool.name(), updatedPool.name());
        assertEquals("This is the description", updatedPool.description());

        // Can't update a non-existent pool
        assertThrows(InvalidInputException.class, () -> poolRepo.update(key, u -> u.name("DoesNotExists")));
        // Can't change the pool type post creation
        assertThrows(InvalidInputException.class, () -> poolRepo.update(key, u -> u.name(devicePool.name())
                .type(DevicePoolType.MANAGED)));

        assertEquals(updatedPool, poolRepo.get(key, devicePool.name()));

        poolRepo.delete(key, devicePool.name());

        assertThrows(NotFoundException.class, () -> poolRepo.get(key, devicePool.name()));
    }

    @Test
    void GIVEN_pool_is_created_WHEN_list_is_invoked_THEN_list_is_paginated_appropriately() {
        CompositeKey account = CompositeKey.of("012345678912");
        assertTrue(poolRepo.list(account, QueryParams.builder().limit(10).build()).results().isEmpty());

        final Set<String> poolNames = spy(createPools(account, 30, "DevicePools"));
        assertEquals(30, poolNames.size());

        QueryResults<DevicePoolObject> results = null;
        int pager = 0;
        do {
            results = poolRepo.list(account, QueryParams.builder()
                    .limit(10)
                    .nextToken(Optional.ofNullable(results).map(QueryResults::nextToken).orElse(null))
                    .build());
            results.results().stream()
                    .map(DevicePoolObject::name)
                    .forEach(poolNames::remove);
            if (results.isTruncated()) {
                pager++;
            }
        } while (results.isTruncated());
        assertEquals(3, pager);
        assertTrue(poolNames.isEmpty(), "List did not page through all items!");
        verify(poolNames, times(30)).remove(anyString());
    }

    @Test
    void GIVEN_pool_is_created_WHEN_marshaller_is_changed_THEN_decryption_error_is_thrown() {
        CompositeKey account1 = CompositeKey.of("account-1");
        CompositeKey account2 = CompositeKey.of("account-2");

        Set<String> accountOnePools = createPools(account1, 20, "AccountOnePools");
        Set<String> accountTwoPools = createPools(account2, 20, "AccountTwoPools");

        assertEquals(20, accountOnePools.size());
        assertEquals(20, accountTwoPools.size());

        QueryResults<DevicePoolObject> one = poolRepo.list(account1, QueryParams.builder()
                .limit(10)
                .build());

        assertEquals(10, one.results().size());
        assertNotNull(one.nextToken());

        assertThrows(InvalidInputException.class, () -> poolRepo.list(account2, QueryParams.builder()
                .limit(10)
                .nextToken(one.nextToken())
                .build()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void GIVEN_pool_is_created_WHEN_client_fails_THEN_repo_wraps_exception() {
        DynamoDbTable<DevicePoolObject> mockedTable = mock(DynamoDbTable.class);
        TokenMarshaller marshaller = mock(TokenMarshaller.class);

        CompositeKey account = CompositeKey.of("012345678912");
        poolRepo = new DevicePoolRepoDynamo(mockedTable, marshaller);

        // Service Failure on Get
        doThrow(DynamoDbException.class).when(mockedTable).getItem(any(Key.class));
        assertThrows(ServiceException.class, () -> poolRepo.get(account, "TestPool"));
        // Service Failure on Create
        doThrow(DynamoDbException.class).when(mockedTable).putItem(any(PutItemEnhancedRequest.class));
        assertThrows(ServiceException.class, () -> poolRepo.create(account, CreateDevicePoolObject.of("TestPool")));
        // Service Failure on List
        doThrow(TokenMarshallerException.class).when(marshaller).unmarshall(any(CompositeKey.class), eq("nextToken"));
        assertThrows(ServiceException.class, () -> poolRepo.list(account, QueryParams.builder()
                .limit(10)
                .nextToken("nextToken")
                .build()));
        // Service Failure on Update
        doThrow(DynamoDbException.class).when(mockedTable).updateItem(any(UpdateItemEnhancedRequest.class));
        assertThrows(ServiceException.class, () -> poolRepo.update(account, UpdateDevicePoolObject.builder()
                .name("TestPool")
                .build()));
        // Service Failure on Delete
        doThrow(DynamoDbException.class).when(mockedTable).deleteItem(any(DeleteItemEnhancedRequest.class));
        assertThrows(ServiceException.class, () -> poolRepo.delete(account, "TestPool"));
    }
}
