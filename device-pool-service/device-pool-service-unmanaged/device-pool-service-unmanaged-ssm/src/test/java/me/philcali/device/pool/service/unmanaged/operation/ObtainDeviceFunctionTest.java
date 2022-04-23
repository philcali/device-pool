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
import me.philcali.device.pool.service.api.model.CreateLockObject;
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
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import me.philcali.device.pool.service.unmanaged.Configuration;
import me.philcali.device.pool.service.unmanaged.ProvisionStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.PingStatus;
import software.amazon.awssdk.services.ssm.model.PlatformType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith({
        MockitoExtension.class,
        DynamoDBExtension.class
})
class ObtainDeviceFunctionTest {
    static DynamoDbTable<LockObject> lockTable;
    static DynamoDbTable<DevicePoolObject> poolTable;
    static DynamoDbTable<ProvisionObject> provisionTable;
    ObtainDeviceFunction function;

    @Mock
    SsmClient ssm;

    LockRepoDynamo lockRepo;
    ProvisionRepoDynamo provisions;
    DevicePoolRepoDynamo poolRepo;
    Configuration configuration;
    ObtainDeviceRequest request;

    @BeforeAll
    static void beforeAll(DynamoDbClient client) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        poolTable = enhancedClient.table("TestTable", TableSchemas.poolTableSchema());
        lockTable = enhancedClient.table("TestTable", TableSchemas.lockSchema());
        provisionTable = enhancedClient.table("TestTable", TableSchemas.provisionTableSchema());
        provisionTable.createTable();
    }

    @BeforeEach
    void setUp() {
        configuration = Configuration.builder()
                .locking(true)
                .lockingDuration(Duration.ofSeconds(15))
                .provisionStrategy(ProvisionStrategy.CYCLIC)
                .build();
        TokenMarshaller marshaller = new EncryptedTokenMarshaller(new ObjectMapper());
        lockRepo = new LockRepoDynamo(lockTable, marshaller);
        provisions = new ProvisionRepoDynamo(provisionTable, marshaller);
        poolRepo = new DevicePoolRepoDynamo(poolTable, marshaller);
        function = new ObtainDeviceFunction(ssm, lockRepo, provisions, poolRepo, configuration);
        request = ObtainDeviceRequest.builder()
                .accountKey(CompositeKey.of("012345678912"))
                .provision(ProvisionObject.builder()
                        .id("abc-123")
                        .status(Status.PROVISIONING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .poolId("pool-id")
                        .build())
                .build();
    }

    @AfterEach
    void teardown() {
        lockRepo.delete(poolRepo.resourceKey(request.accountKey(), "pool-id"), LockRepoDynamo.SINGLETON);
    }

    @Test
    void GIVEN_function_is_created_WHEN_apply_is_called_THEN_cycles_through_instances() {
        // If not locking, then don't adhere to lock
        lockRepo.create(poolRepo.resourceKey(request.accountKey(), "pool-id"), CreateLockObject.builder()
                .holder(UUID.randomUUID().toString())
                .duration(Duration.ofSeconds(5))
                .build());
        function = new ObtainDeviceFunction(ssm, lockRepo, provisions, poolRepo, Configuration.builder().from(configuration).locking(false).build());
        DescribeInstanceInformationRequest firstRequest = DescribeInstanceInformationRequest.builder()
                .maxResults(5)
                .filters(
                        filter -> filter.key("tag:DevicePool").values("pool-id")
                )
                .build();
        DescribeInstanceInformationRequest secondRequest = DescribeInstanceInformationRequest.builder()
                .maxResults(5)
                .nextToken("nextToken")
                .filters(
                        filter -> filter.key("tag:DevicePool").values("pool-id")
                )
                .build();
        DescribeInstanceInformationResponse firstResponse = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.instanceId("device-1").pingStatus(PingStatus.ONLINE).ipAddress("127.0.0.1").platformType(PlatformType.LINUX),
                        instance -> instance.instanceId("device-2").pingStatus(PingStatus.ONLINE).ipAddress("127.0.0.1").platformType(PlatformType.LINUX),
                        instance -> instance.instanceId("device-3").pingStatus(PingStatus.ONLINE).ipAddress("127.0.0.1").platformType(PlatformType.LINUX),
                        instance -> instance.instanceId("device-4").pingStatus(PingStatus.ONLINE).ipAddress("127.0.0.1").platformType(PlatformType.LINUX),
                        instance -> instance.instanceId("device-5").pingStatus(PingStatus.ONLINE).ipAddress("127.0.0.1").platformType(PlatformType.LINUX)
                )
                .nextToken("nextToken")
                .build();
        DescribeInstanceInformationResponse secondResponse = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(
                        instance -> instance.instanceId("device-6").pingStatus(PingStatus.ONLINE).ipAddress("192.168.1.1").platformType(PlatformType.LINUX)
                )
                .build();
        when(ssm.describeInstanceInformation(any(DescribeInstanceInformationRequest.class))).then(answer -> {
            DescribeInstanceInformationRequest request = answer.getArgument(0);
            if (request.equals(firstRequest)) {
                return firstResponse;
            } else {
                return secondResponse;
            }
        });
        List<InstanceInformation> allInstances = new ArrayList<>();
        allInstances.addAll(firstResponse.instanceInformationList());
        allInstances.addAll(secondResponse.instanceInformationList());
        for (int i = 1; i <= allInstances.size(); i++) {
            ObtainDeviceResponse obtainResponse = function.apply(request);
            assertEquals(Status.SUCCEEDED, obtainResponse.status());
            assertEquals("device-" + i, obtainResponse.device().id());
            assertEquals("pool-id", obtainResponse.device().poolId());
            assertEquals(allInstances.get(i - 1).ipAddress(), obtainResponse.device().publicAddress());
        }
        // Cycles to first
        DeviceObject firstDevice = function.apply(request).device();
        assertEquals("device-1", firstDevice.id());
        assertEquals("pool-id", firstDevice.poolId());
        assertEquals(allInstances.get(0).ipAddress(), firstDevice.publicAddress());
    }

    @Test
    void GIVEN_function_is_created_WHEN_apply_is_called_but_no_instances_THEN_throws_exception() {
        DescribeInstanceInformationRequest firstRequest = DescribeInstanceInformationRequest.builder()
                .maxResults(5)
                .filters(
                        filter -> filter.key("tag:DevicePool").values("pool-id")
                )
                .build();
        DescribeInstanceInformationResponse response = DescribeInstanceInformationResponse.builder()
                .instanceInformationList(Collections.emptyList())
                .build();
        doReturn(response).when(ssm).describeInstanceInformation(eq(firstRequest));
        assertThrows(IllegalStateException.class, () -> function.apply(request));
    }

    @Test
    void GIVEN_function_is_created_WHEN_random_algo_is_used_THEN_throws_exception() {
        function = new ObtainDeviceFunction(ssm, lockRepo, provisions, poolRepo, Configuration.builder()
                .from(configuration).provisionStrategy(ProvisionStrategy.RANDOM).build());
        assertThrows(IllegalArgumentException.class, () -> function.apply(request));
    }
}
