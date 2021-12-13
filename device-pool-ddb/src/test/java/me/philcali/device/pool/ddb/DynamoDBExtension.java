package me.philcali.device.pool.ddb;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

public class DynamoDBExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final int PORT = 8080;
    private DynamoDBProxyServer server;
    private DynamoDbClient ddb;

    static {
        System.setProperty("sqlite4java.library.path", "native-libs");
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        server = ServerRunner.createServerFromCommandLineArgs(new String[] {
                "-inMemory",
                "-port", Integer.toString(PORT)
        });
        server.start();
        ddb = DynamoDbClient.builder()
                .endpointOverride(new URI("http://localhost:" + PORT))
                .build();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        server.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == DynamoDbClient.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return ddb;
    }
}
