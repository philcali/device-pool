/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
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
class ReservationRepoDynamoTest {
    static DynamoDbTable<ReservationObject> table;
    ReservationRepo reservations;

    @BeforeAll
    static void beforeAll(DynamoDbClient ddb) {
        DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        table = client.table("TestTable", TableSchemas.reservationTableSchema());
        table.createTable();
    }

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        reservations = new ReservationRepoDynamo(table, new EncryptedTokenMarshaller(new ObjectMapper()));
    }

    @Test
    void GIVEN_repo_is_created_WHEN_list_is_invoked_THEN_list_is_paginated_appropriately() {
        CompositeKey key = CompositeKey.builder()
                .account("012345678912")
                .addResources("pool")
                .addResources("poolId")
                .addResources("provision")
                .addResources("provisionId")
                .build();

        ReservationObject reservation = reservations.create(key, CreateReservationObject.builder()
                .id("abc-123")
                .deviceId("deviceId")
                .build());

        assertEquals(reservation, reservations.get(key, reservation.id()));

        ReservationObject updated = reservations.update(key, UpdateReservationObject.builder()
                .id("abc-123")
                .status(Status.PROVISIONING)
                .build());

        assertEquals(updated, reservations.get(key, reservation.id()));
    }
}
