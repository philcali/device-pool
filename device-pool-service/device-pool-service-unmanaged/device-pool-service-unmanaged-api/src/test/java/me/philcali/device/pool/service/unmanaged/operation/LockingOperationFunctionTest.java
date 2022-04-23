/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.LockRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.data.TableSchemas;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import me.philcali.device.pool.service.rpc.model.AccountRequest;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.unmanaged.model.LockingConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({DynamoDBExtension.class, MockitoExtension.class})
class LockingOperationFunctionTest {
    static DynamoDbTable<DevicePoolObject> poolTable;
    static DynamoDbTable<ProvisionObject> provisionTable;
    static DynamoDbTable<LockObject> lockTable;

    @Mock
    LockingConfiguration configuration;

    LockingOperationFunction<AccountRequest> lockFunction;

    DevicePoolRepoDynamo poolRepo;
    ProvisionRepoDynamo provisionDynamo;
    LockRepoDynamo spiedRepo;

    @BeforeAll
    static void beforeAll(DynamoDbClient client) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        poolTable = enhancedClient.table("TestTable", TableSchemas.poolTableSchema());
        provisionTable = enhancedClient.table("TestTable", TableSchemas.provisionTableSchema());
        lockTable = enhancedClient.table("TestTable", TableSchemas.lockSchema());
        poolTable.createTable();
    }

    @BeforeEach
    void setUp() {
        TokenMarshaller tokenMarshaller = new EncryptedTokenMarshaller(new ObjectMapper());
        poolRepo = new DevicePoolRepoDynamo(poolTable, tokenMarshaller);
        provisionDynamo = new ProvisionRepoDynamo(provisionTable, tokenMarshaller);
        spiedRepo = spy(new LockRepoDynamo(lockTable, tokenMarshaller));
        lockFunction = new LockingOperationFunction<>(poolRepo, provisionDynamo, spiedRepo, configuration);
    }

    @Test
    void GIVEN_lock_function_WHEN_lock_is_disabled_THEN_lock_call_is_skipped() {
        doReturn(false).when(configuration).locking();
        ObtainDeviceRequest obtainDeviceRequest = ObtainDeviceRequest.builder()
                .accountKey(CompositeKey.of("012345678912"))
                .provision(ProvisionObject.builder()
                        .id("provisionId")
                        .amount(1)
                        .poolId("poolId")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .status(Status.PROVISIONING)
                        .build())
                .build();
        assertEquals(obtainDeviceRequest, lockFunction.apply(obtainDeviceRequest));
        verify(configuration, times(0)).lockingDuration();
        CompositeKey poolKey = poolRepo.resourceKey(obtainDeviceRequest.accountKey(), "poolId");
        verify(spiedRepo, times(0)).extend(eq(poolKey), any(CreateLockObject.class));
    }

    @Test
    void GIVEN_lock_function_WHEN_lock_is_applying_THEN_pool_is_locked() {
        doReturn(true).when(configuration).locking();
        doReturn(Duration.ofSeconds(10)).when(configuration).lockingDuration();

        ObtainDeviceRequest obtainDeviceRequest = ObtainDeviceRequest.builder()
                .accountKey(CompositeKey.of("012345678912"))
                .provision(ProvisionObject.builder()
                        .id("provisionId")
                        .amount(1)
                        .poolId("poolId")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .status(Status.PROVISIONING)
                        .build())
                .build();
        CompositeKey poolKey = poolRepo.resourceKey(obtainDeviceRequest.accountKey(), "poolId");
        assertEquals(obtainDeviceRequest, lockFunction.apply(obtainDeviceRequest));
        verify(spiedRepo).extend(eq(poolKey), any(CreateLockObject.class));

        CancelReservationRequest cancelRequest = CancelReservationRequest.builder()
                .accountKey(obtainDeviceRequest.accountKey())
                .provision(obtainDeviceRequest.provision())
                .reservation(ReservationObject.builder()
                        .id("reservationId")
                        .deviceId("deviceId")
                        .poolId("poolId")
                        .provisionId("provisionId")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .status(Status.PROVISIONING)
                        .build())
                .build();
        assertEquals(cancelRequest, lockFunction.apply(cancelRequest));

        assertThrows(InvalidInputException.class, () -> lockFunction.apply(ObtainDeviceRequest.builder()
                .accountKey(obtainDeviceRequest.accountKey())
                .provision(ProvisionObject.builder()
                        .id("anotherId")
                        .poolId("poolId")
                        .status(Status.PROVISIONING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .build()));
    }
}
