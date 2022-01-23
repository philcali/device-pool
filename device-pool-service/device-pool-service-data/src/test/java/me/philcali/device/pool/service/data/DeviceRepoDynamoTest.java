/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.exception.ConflictException;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({DynamoDBExtension.class})
class DeviceRepoDynamoTest {
    static DynamoDbTable<DeviceObject> table;
    DeviceRepo devices;

    @BeforeAll
    static void beforeAll(DynamoDbClient ddb) {
        DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        table = client.table("TestTable", TableSchemas.deviceSchema());
        table.createTable();
    }

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        devices = new DeviceRepoDynamo(table, new EncryptedTokenMarshaller(new ObjectMapper()));
    }

    @Test
    void GIVEN_repo_is_created_WHEN_management_workflow_is_exercised_THEN_create_read_update_delete_contract() {
        CompositeKey poolKey = CompositeKey.builder()
                .account("012345678912")
                .addResources("DevicePools")
                .addResources("poolId")
                .build();

        assertThrows(InvalidInputException.class, () -> devices.update(poolKey, UpdateDeviceObject.builder()
                .id("ny-device")
                .build()));

        DeviceObject newDevice = devices.create(poolKey, CreateDeviceObject.builder()
                .id("my-device")
                .privateAddress("127.0.0.1")
                .publicAddress("0.0.0.0")
                .build());

        assertThrows(ConflictException.class, () -> devices.create(poolKey, CreateDeviceObject.builder()
                .id(newDevice.id())
                .privateAddress(newDevice.privateAddress())
                .publicAddress(newDevice.publicAddress())
                .build()));

        assertEquals("my-device", newDevice.id());
        assertEquals("poolId", newDevice.poolId());

        assertEquals(newDevice, devices.get(poolKey, newDevice.id()));

        DeviceObject updatedDevice = devices.update(poolKey, UpdateDeviceObject.builder()
                .id(newDevice.id())
                .publicAddress("10.0.0.1")
                .build());

        assertEquals(updatedDevice, devices.get(poolKey, newDevice.id()));

        devices.delete(poolKey, newDevice.id());

        assertThrows(NotFoundException.class, () -> devices.get(poolKey, newDevice.id()));
    }

    Set<String> createDevices(CompositeKey account, int number, String prefix) {
        Set<String> ids = new HashSet<>();
        IntStream.range(0, number).forEach(index -> {
            ids.add(devices.create(account, CreateDeviceObject.builder()
                            .id(prefix + index)
                            .publicAddress("0.0.0.0")
                            .privateAddress("127.0.0.1")
                            .build())
                    .id());
        });
        return ids;
    }

    @Test
    void GIVEN_repo_is_created_WHEN_list_is_invoked_THEN_list_is_paginated_appropriately() {
        CompositeKey account = CompositeKey.builder().account("012345678912").addResources("Pools", "poolId").build();
        assertTrue(devices.list(account, QueryParams.builder().limit(10).build()).results().isEmpty());

        final Set<String> poolNames = spy(createDevices(account, 30, "DevicePools"));
        assertEquals(30, poolNames.size());

        QueryResults<DeviceObject> results = null;
        int pager = 0;
        do {
            results = devices.list(account, QueryParams.builder()
                    .limit(10)
                    .nextToken(Optional.ofNullable(results).map(QueryResults::nextToken).orElse(null))
                    .build());
            results.results().stream()
                    .map(DeviceObject::id)
                    .forEach(poolNames::remove);
            if (results.isTruncated()) {
                pager++;
            }
        } while (results.isTruncated());
        assertEquals(3, pager);
        assertTrue(poolNames.isEmpty(), "List did not page through all items!");
        verify(poolNames, times(30)).remove(anyString());
    }
}
