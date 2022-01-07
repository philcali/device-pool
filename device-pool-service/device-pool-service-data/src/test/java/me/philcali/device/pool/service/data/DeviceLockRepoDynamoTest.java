package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.service.api.DeviceLockRepo;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceLockObject;
import me.philcali.device.pool.service.api.model.DeviceLockObject;
import me.philcali.device.pool.service.api.model.UpdateDeviceLockObject;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({DynamoDBExtension.class})
class DeviceLockRepoDynamoTest {
    static DynamoDbTable<DeviceLockObject> table;
    private DeviceLockRepo deviceLockRepo;

    @BeforeAll
    static void beforeAll(DynamoDbClient client) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        table = enhancedClient.table("TestTable", TableSchemas.deviceLockSchema());
        table.createTable();
    }

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        deviceLockRepo = new DeviceLockRepoDynamo(table, new EncryptedTokenMarshaller(new ObjectMapper()));
    }

    @Test
    void GIVEN_repo_is_created_WHEN_management_workflow_is_exercised_THEN_create_read_update_delete_contract() {
        CompositeKey account = CompositeKey.builder()
                .account("012345678912")
                .addResources(
                        DevicePoolRepoDynamo.RESOURCE, "pool-id",
                        DeviceRepoDynamo.RESOURCE, "device-id")
                .build();

        assertThrows(IllegalStateException.class, () -> CreateDeviceLockObject.of("a", "b", "c"));

        DeviceLockObject lockObject = deviceLockRepo.create(account, CreateDeviceLockObject.builder()
                .id("lock")
                .provisionId("pid")
                .reservationId("rid")
                .duration(Duration.ofSeconds(10))
                .build());

        assertEquals(lockObject, deviceLockRepo.get(account, "lock"));

        DeviceLockObject updated = deviceLockRepo.update(account, UpdateDeviceLockObject.builder()
                .id("lock")
                .provisionId("pid")
                .reservationId("rid")
                .expiresIn(Instant.now().plus(20, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS))
                .build());

        assertEquals(updated, deviceLockRepo.get(account, "lock"));

        // Lock is being held
        assertThrows(InvalidInputException.class, () -> deviceLockRepo.update(account, UpdateDeviceLockObject.builder()
                .id("lock")
                .provisionId("pid2")
                .reservationId("rid2")
                .expiresIn(Instant.now().plus(20, ChronoUnit.SECONDS))
                .build()));
    }
}
