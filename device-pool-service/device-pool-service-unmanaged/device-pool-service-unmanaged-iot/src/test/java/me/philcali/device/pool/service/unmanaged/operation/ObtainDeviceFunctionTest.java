/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.LockRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.data.TableSchemas;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.unmanaged.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsInThingGroupRequest;
import software.amazon.awssdk.services.iot.model.ListThingsInThingGroupResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({DynamoDBExtension.class, MockitoExtension.class})
class ObtainDeviceFunctionTest {
    static DynamoDbTable<LockObject> lockTable;
    static DynamoDbTable<DevicePoolObject> poolTable;
    static DynamoDbTable<ProvisionObject> provisionTable;
    ObtainDeviceFunction function;

    @Mock
    IotClient client;

    @BeforeAll
    static void beforeAll(DynamoDbClient ddb) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        poolTable = enhancedClient.table("TestTable", TableSchemas.poolTableSchema());
        lockTable = enhancedClient.table("TestTable", TableSchemas.lockSchema());
        provisionTable = enhancedClient.table("TestTable", TableSchemas.provisionTableSchema());
        provisionTable.createTable();
    }

    @BeforeEach
    void setUp() {
        TokenMarshaller tokenMarshaller = new EncryptedTokenMarshaller(new ObjectMapper());
        Configuration configuration = Configuration.builder()
                .locking(false)
                .lockingDuration(Duration.ofSeconds(15))
                .recursive(true)
                .build();
        LockRepoDynamo lockRepoDynamo = new LockRepoDynamo(lockTable, tokenMarshaller);
        DevicePoolRepoDynamo poolRepoDynamo = new DevicePoolRepoDynamo(poolTable, tokenMarshaller);
        ProvisionRepoDynamo provisionRepoDynamo = new ProvisionRepoDynamo(provisionTable, tokenMarshaller);
        function = new ObtainDeviceFunction(client, configuration, lockRepoDynamo, provisionRepoDynamo, poolRepoDynamo);
    }

    @Test
    void GIVEN_function_is_created_WHEN_function_returns_no_things_THEN_error_is_thrown() {
        ListThingsInThingGroupResponse response = ListThingsInThingGroupResponse.builder()
                .things(Collections.emptyList())
                .build();
        doReturn(response).when(client).listThingsInThingGroup(eq(ListThingsInThingGroupRequest.builder()
                .maxResults(1)
                .recursive(true)
                .thingGroupName("poolId2")
                .build()));
        ObtainDeviceRequest request = ObtainDeviceRequest.builder()
                .accountKey(CompositeKey.of("012345678912"))
                .provision(ProvisionObject.builder()
                        .status(Status.PROVISIONING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .id("provisionId2")
                        .poolId("poolId2")
                        .build())
                .build();
        assertThrows(IllegalStateException.class, () -> function.apply(request));
    }

    @Test
    void GIVEN_function_is_created_WHEN_function_is_called_THEN_devices_cycled() {
        ListThingsInThingGroupResponse response = ListThingsInThingGroupResponse.builder()
                .things("Thing1")
                .nextToken("nextToken")
                .build();
        doReturn(response).when(client).listThingsInThingGroup(eq(ListThingsInThingGroupRequest.builder()
                .maxResults(1)
                .recursive(true)
                .thingGroupName("poolId")
                .build()));
        ListThingsInThingGroupResponse response2 = ListThingsInThingGroupResponse.builder()
                .things("Thing2")
                .build();
        doReturn(response2).when(client).listThingsInThingGroup(eq(ListThingsInThingGroupRequest.builder()
                .maxResults(1)
                .recursive(true)
                .thingGroupName("poolId")
                .nextToken("nextToken")
                .build()));

        ObtainDeviceRequest request = ObtainDeviceRequest.builder()
                .accountKey(CompositeKey.of("012345678912"))
                .provision(ProvisionObject.builder()
                        .status(Status.PROVISIONING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .id("provisionId")
                        .poolId("poolId")
                        .build())
                .build();
        DeviceObject firstDevice = function.apply(request).device();
        assertEquals("Thing1", firstDevice.id());
        assertEquals("Thing1", firstDevice.publicAddress());
        DeviceObject secondDevice = function.apply(request).device();
        assertEquals("Thing2", secondDevice.id());
        assertEquals("Thing2", secondDevice.publicAddress());
        DeviceObject repeatDevice = function.apply(request).device();
        assertEquals(firstDevice.id(), repeatDevice.id());
        assertEquals(firstDevice.publicAddress(), repeatDevice.publicAddress());
    }
}
