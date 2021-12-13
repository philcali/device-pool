package me.philcali.device.pool.ddb;

import me.philcali.device.pool.lock.LockingMechanism;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.LockOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({DynamoDBExtension.class})
class LockingMechanismDynamoDBTest {
    private static final String LOCK_TABLE = "LockTable";
    private LockingMechanism mechanism;
    private ScheduledExecutorService scheduler;

    @BeforeAll
    static void createTable(DynamoDbClient ddb) {
        ddb.createTable(CreateTableRequest.builder()
                .tableName(LOCK_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build())
                .build());
    }

    @BeforeEach
    void setup(DynamoDbClient ddb) {
        mechanism = LockingMechanismDynamoDB.builder()
                .dynamoDbClient(ddb)
                .tableName(LOCK_TABLE)
                .build();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void teardown() {
        scheduler.shutdown();
    }

    @Test
    void GIVEN_locking_mechanism_is_created_WHEN_lock_THEN_conditional_checks_are_adhered_to()
            throws ExecutionException, InterruptedException, TimeoutException {
        Instant now = Instant.now();
        LockInput input = LockInput.builder()
                .id("test-id")
                .ttl(TimeUnit.SECONDS.toSeconds(15))
                .value("MyValue")
                .holder("first-holder")
                .build();
        CompletableFuture<LockOutput> future = mechanism.lock(input);
        LockOutput output = future.get(5, TimeUnit.SECONDS);

        assertEquals(input.id(), output.id());
        assertEquals(input.value(), output.value());
        assertTrue(output.expiresIn() >= now.plus(15, ChronoUnit.SECONDS).getEpochSecond());
    }

    @Test
    void GIVEN_locking_mechanism_is_created_WHEN_lock_THEN_conditional_checks_fails_for_timeout()
            throws ExecutionException, InterruptedException, TimeoutException {
        LockInput input = LockInput.builder()
                .id("test-id")
                .ttl(TimeUnit.SECONDS.toSeconds(15))
                .value("MyValue")
                .holder("first-holder")
                .build();
        LockInput second = LockInput.builder()
                .id("test-id")
                .ttl(TimeUnit.SECONDS.toSeconds(15))
                .value("MyOtherValue")
                .holder("second-holder")
                .build();
        // locks the first
        mechanism.lock(input).get(5, TimeUnit.SECONDS);
        // creates a poller for the second lock
        CompletableFuture<LockOutput> secondFuture = mechanism.lock(second);
        assertThrows(TimeoutException.class, () -> secondFuture.get(1, TimeUnit.SECONDS));
    }

    @Test
    void GIVEN_locking_mechanism_is_created_WHEN_lock_THEN_conditional_checks_pass_if_lock_expires()
            throws ExecutionException, InterruptedException, TimeoutException {
        LockInput input = LockInput.builder()
                .id("test-id")
                .ttl(TimeUnit.SECONDS.toSeconds(15))
                .value("MyValue")
                .holder("first-holder")
                .build();
        LockInput second = LockInput.builder()
                .id("test-id")
                .ttl(TimeUnit.SECONDS.toSeconds(15))
                .value("MyOtherValue")
                .holder("second-holder")
                .build();
        // locks the first
        mechanism.lock(input).get(5, TimeUnit.SECONDS);
        // creates a poller for the second lock
        CompletableFuture<LockOutput> secondFuture = mechanism.lock(second);
        // create a time-bomb to clear the lock, one second from now
        scheduler.schedule(() -> mechanism.lease(input.id()), 1, TimeUnit.SECONDS);
        // Blocks until lock is released
        LockOutput output = secondFuture.get(5, TimeUnit.SECONDS);
        assertEquals(second.value(), output.value());
    }
}
