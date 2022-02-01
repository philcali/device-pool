/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateLockObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({DynamoDBExtension.class})
class LockRepoDynamoTest {
    static DynamoDbTable<LockObject> table;
    LockRepo lockRepo;

    @BeforeAll
    static void setupAll(DynamoDbClient client) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        table = enhancedClient.table("TestTable", TableSchemas.lockSchema());
        table.createTable();
    }

    @BeforeEach
    void setup() {
        lockRepo = new LockRepoDynamo(table, new EncryptedTokenMarshaller(new ObjectMapper()));
    }

    @Test
    void GIVEN_pool_is_created_WHEN_management_workflow_is_exercised_THEN_create_read_update_delete_contract() {
        CompositeKey key = CompositeKey.builder()
                .account("012345678912")
                .addResources("pool")
                .addResources("abc-123")
                .addResources("provision")
                .addResources("efg-456")
                .addResources("reservation")
                .addResources("xyz-789")
                .build();

        String holder = UUID.randomUUID().toString();
        LockObject lockObject = lockRepo.create(key, CreateLockObject.builder()
                .id("abc-123")
                .holder(holder)
                .duration(Duration.ofSeconds(10))
                .build());

        assertEquals(lockObject, lockRepo.get(key, lockObject.id()));

        LockObject updated = lockRepo.update(key, UpdateLockObject.builder()
                .id(lockObject.id())
                .holder(holder)
                .expiresIn(Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS))
                .build());

        assertEquals(updated, lockRepo.get(key, lockObject.id()));

        lockRepo.delete(key, updated.id());

        assertThrows(NotFoundException.class, () -> lockRepo.get(key, updated.id()));
    }
}
