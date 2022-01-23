package me.philcali.device.pool.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.philcali.device.pool.ddb.DynamoDBExtension;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolLockOptions;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;
import me.philcali.device.pool.service.data.TableSchemas;
import me.philcali.device.pool.service.local.Server;
import me.philcali.device.pool.service.module.DaggerDevicePoolsComponent;
import me.philcali.device.pool.service.module.DevicePoolsComponent;
import me.philcali.device.pool.service.module.DynamoDBModule;
import me.philcali.device.pool.service.module.JacksonModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Extensions({
        @ExtendWith({DynamoDBExtension.class})
})
class DeviceLabServiceTest {
    static DeviceLabService service;
    static Server localServer;

    @BeforeAll
    static void beforeAll(DynamoDbClient client, AwsCredentialsProvider credentialsProvider) throws IOException {
        String endpoint = "http://localhost:8000";
        String tableName = "DeviceLab";
        DevicePoolsComponent component = DaggerDevicePoolsComponent.builder()
                .dynamoDBModule(new DynamoDBModule(client, tableName))
                .jacksonModule(new JacksonModule())
                .build();
        localServer = Server.builder()
                .endpoint(endpoint)
                .component(component)
                .build();
        CreateTables.createTableFromSchema(client, tableName, TableSchemas.poolTableSchema());
        service = DeviceLabService.create((httpClient, retrofit) -> {
            httpClient.addInterceptor(new AwsV4SigningInterceptor(
                    credentialsProvider,
                    () -> Region.US_EAST_1,
                    Aws4Signer.create()
            ));
            httpClient.addInterceptor(chain -> {
                assertNotNull(
                        chain.request().header("Authorization"),
                        "Request is not signed");
                System.out.println("Request: " + chain.request());
                return chain.proceed(chain.request());
            });
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            retrofit.baseUrl(endpoint)
                    .addConverterFactory(JacksonConverterFactory.create(mapper));
        });
        localServer.start();
    }

    @AfterAll
    static void teardown() {
        if (Objects.nonNull(localServer)) {
            localServer.stop();
        }
    }

    private <T> T executeAndReturn(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        assertTrue(response.isSuccessful(), "Response "
                + response.code() + " was not successful "
                + response.errorBody());
        return response.body();
    }

    @Test
    void GIVEN_local_server_is_running_WHEN_client_calls_THEN_service_works() throws IOException {
        Call<DevicePoolObject> createCall = service.createDevicePool(CreateDevicePoolObject.builder()
                .name("IntegPool")
                .description("This is a local device pool")
                .type(DevicePoolType.MANAGED)
                .build());
        DevicePoolObject created = executeAndReturn(createCall);

        Call<QueryResults<DevicePoolObject>> listCall = service.listDevicePools(QueryParams.of(10));
        assertEquals(QueryResults.builder()
                .addResults(created)
                .isTruncated(false)
                .build(), executeAndReturn(listCall));

        Call<DevicePoolObject> getCall = service.getDevicePool(created.id());
        assertEquals(created, executeAndReturn(getCall));

        Call<DevicePoolObject> updateCall = service.updateDevicePool(created.id(), UpdateDevicePoolObject.builder()
                .description("An updated pool")
                .lockOptions(DevicePoolLockOptions.of(false))
                .build());
        DevicePoolObject updated = executeAndReturn(updateCall);

        Call<DevicePoolObject> getUpdatedCall = service.getDevicePool(created.id());
        assertEquals(updated, executeAndReturn(getUpdatedCall));

        Call<DeviceObject> createDeviceCall = service.createDevice(updated.id(), CreateDeviceObject.builder()
                .id("my-instance-123")
                .privateAddress("127.0.0.1")
                .publicAddress("192.168.1.1")
                .build());
        DeviceObject createdDevice = executeAndReturn(createDeviceCall);

        Call<QueryResults<DeviceObject>> listDevices = service.listDevices(updated.id(), QueryParams.of(10));
        assertEquals(QueryResults.builder()
                .addResults(createdDevice)
                .isTruncated(false)
                .build(), executeAndReturn(listDevices));

        Call<DeviceObject> updatedDeviceCall = service.updateDevice(updated.id(), createdDevice.id(), UpdateDeviceObject.builder()
                .publicAddress("192.168.1.206")
                .build());
        DeviceObject updatedDevice = executeAndReturn(updatedDeviceCall);
        assertEquals(updatedDevice, executeAndReturn(service.getDevice(updated.id(), updatedDevice.id())));

        Call<ProvisionObject> createProvisionCall = service.createProvision(updated.id(), CreateProvisionObject.builder()
                .id(UUID.randomUUID().toString())
                .amount(1)
                .build());
        ProvisionObject createdProvision = executeAndReturn(createProvisionCall);

        Call<QueryResults<ProvisionObject>> listProvisionCall = service.listProvisions(updated.id(), QueryParams.of(10));
        assertEquals(QueryResults.builder()
                .addResults(createdProvision)
                .isTruncated(false)
                .build(), executeAndReturn(listProvisionCall));

        Call<Void> deleteProvisionCall = service.deleteProvision(updated.id(), createdProvision.id());
        Response<Void> response = deleteProvisionCall.execute();
        assertFalse(response.isSuccessful(), "Provision was able to delete when it should not have.");

        executeAndReturn(service.deleteDevice(updated.id(), updatedDevice.id()));
        executeAndReturn(service.deleteDevicePool(updated.id()));
    }
}
