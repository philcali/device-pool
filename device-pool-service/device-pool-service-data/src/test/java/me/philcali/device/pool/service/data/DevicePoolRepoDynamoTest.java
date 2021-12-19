package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        ObjectMapper mapper = new ObjectMapper();
        poolRepo = new DevicePoolRepoDynamo(objectTable, new EncryptedTokenMarshaller(mapper));
    }

    @Test
    void GIVEN_pool_is_created_WHEN_management_workflow_is_exercised_THEN_create_read_update_delete_contract() {
        CompositeKey key = CompositeKey.of("abc123456789");

        DevicePoolObject devicePool = poolRepo.create(key, CreateDevicePoolObject.builder()
                .name("TestDevicePool")
                .build());

        assertNotNull(devicePool.id());
        assertEquals(key.account(), devicePool.account().account());
        assertEquals("TestDevicePool", devicePool.name());

        DevicePoolObject obtainedObject = poolRepo.get(key, devicePool.id());

        assertEquals(devicePool, obtainedObject);
    }
}
