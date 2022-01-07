package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import me.philcali.device.pool.service.api.exception.ServiceException;
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
import java.security.NoSuchAlgorithmException;

@Module
class DynamoDBModule {
    private static final String TABLE_NAME = "TABLE_NAME";

    @Provides
    @Singleton
    static DynamoDbClient providesDynamoDBClient() {
        return DynamoDbClient.create();
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
    static String providesTableName() {
        return System.getenv(TABLE_NAME);
    }

    @Provides
    @Singleton
    static TokenMarshaller providesTokenMarshaller(final ObjectMapper mapper) {
        try {
            return new EncryptedTokenMarshaller(mapper);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException(e);
        }
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
