package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
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

@ExtendWith({DynamoDBExtension.class})
class ProvisionRepoDynamoTest {
    static DynamoDbTable<ProvisionObject> table;
    ProvisionRepo provisionRepo;

    @BeforeAll
    static void beforeAll(DynamoDbClient ddb) {
        DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        table = client.table("TestTable", TableSchemas.provisionTableSchema());
        table.createTable();
    }

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        provisionRepo = new ProvisionRepoDynamo(table, new EncryptedTokenMarshaller(new ObjectMapper()));
    }

    @Test
    void GIVEN_repo_is_created_WHEN_list_is_invoked_THEN_list_is_paginated_appropriately() {
        CompositeKey key = CompositeKey.builder()
                .account("012345678912")
                .addResources("pool")
                .addResources("abc-123")
                .build();

        ProvisionObject provision = provisionRepo.create(key, CreateProvisionObject.builder()
                .id("abc-123")
                .amount(2)
                .build());

        assertEquals(provision, provisionRepo.get(key, provision.id()));

        ProvisionObject updated = provisionRepo.update(key, UpdateProvisionObject.builder()
                .id(provision.id())
                .status(Status.REQUESTED)
                .build());

        assertEquals(updated, provisionRepo.get(key, provision.id()));
    }
}
