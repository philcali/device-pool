/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ddb;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

public class DynamoDBExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final int PORT = 8080;
    private DynamoDBProxyServer server;
    private DynamoDbClient ddb;
    private AwsCredentialsProvider mockProvider;

    static {
        System.setProperty("sqlite4java.library.path", "native-libs");
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        String port = extensionContext.getConfigurationParameter("ddb.port").orElseGet(() -> Integer.toString(PORT));
        server = ServerRunner.createServerFromCommandLineArgs(new String[] {
                "-inMemory",
                "-port", port
        });
        server.start();
        mockProvider = () -> new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return extensionContext.getDisplayName();
            }

            @Override
            public String secretAccessKey() {
                return extensionContext.getUniqueId();
            }
        };
        ddb = DynamoDbClient.builder()
                .endpointOverride(new URI("http://localhost:" + PORT))
                .credentialsProvider(mockProvider)
                .region(Region.US_EAST_1)
                .build();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        server.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == DynamoDbClient.class
                || parameterContext.getParameter().getType() == AwsCredentialsProvider.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType() == DynamoDbClient.class) {
            return ddb;
        } else {
            return mockProvider;
        }
    }
}
