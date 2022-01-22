package me.philcali.device.pool.service.client;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

final class CreateTables {
    static void createTableFromSchema(DynamoDbClient client, String tableName, TableSchema<?> schema) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        enhancedClient.table(tableName, schema).createTable();
    }
}
