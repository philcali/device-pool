package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import me.philcali.device.pool.service.api.model.DeviceLockObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.data.TableSchemas;
import me.philcali.device.pool.service.data.token.EncryptedTokenMarshaller;
import me.philcali.device.pool.service.data.token.TokenMarshaller;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class DynamoDBModule {
    private static final String TABLE_NAME = "TABLE_NAME";
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDBModule(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    DynamoDBModule() {
        this(DynamoDbClient.create(), System.getenv(TABLE_NAME));
    }

    @Provides
    @Singleton
    DynamoDbClient providesDynamoDBClient() {
        return dynamoDbClient;
    }

    @Provides
    @Singleton
    static DynamoDbEnhancedClient providesEnhancedClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Provides
    @Singleton
    @Named(TABLE_NAME)
    String providesTableName() {
        return tableName;
    }

    @Provides
    @Singleton
    static TokenMarshaller providesTokenMarshaller(final ObjectMapper mapper) {
        return new EncryptedTokenMarshaller(mapper);
    }

    @Provides
    @Singleton
    static DynamoDbTable<DevicePoolObject> providesDevicePoolTable(
            @Named(TABLE_NAME) final String tableName,
            final DynamoDbEnhancedClient client) {
        return client.table(tableName, TableSchemas.poolTableSchema());
    }

    @Provides
    @Singleton
    static DynamoDbTable<DeviceObject> providesDeviceTable(
            @Named(TABLE_NAME) final String tableName,
            final DynamoDbEnhancedClient client) {
        return client.table(tableName, TableSchemas.deviceSchema());
    }

    @Provides
    @Singleton
    static DynamoDbTable<ProvisionObject> providesProvisionTable(
            @Named(TABLE_NAME) final String tableName,
            final DynamoDbEnhancedClient client) {
        return client.table(tableName, TableSchemas.provisionTableSchema());
    }

    @Provides
    @Singleton
    static DynamoDbTable<ReservationObject> providesReservationTable(
            @Named(TABLE_NAME) final String tableName,
            final DynamoDbEnhancedClient client) {
        return client.table(tableName, TableSchemas.reservationTableSchema());
    }

    @Provides
    @Singleton
    static DynamoDbTable<DeviceLockObject> providesDeviceLockTable(
            @Named(TABLE_NAME) final String tableName,
            final DynamoDbEnhancedClient client) {
        return client.table(tableName, TableSchemas.deviceLockSchema());
    }
}
