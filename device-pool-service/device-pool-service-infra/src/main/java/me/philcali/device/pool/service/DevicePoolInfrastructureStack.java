package me.philcali.device.pool.service;

import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigateway.AuthorizationType;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.StreamViewType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.StreamEventSource;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.IntStream;

public class DevicePoolInfrastructureStack extends Stack {

    public DevicePoolInfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table table = Table.Builder.create(this, "DevicePoolTable")
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(1)
                .writeCapacity(1)
                .tableName("DeviceLab")
                .timeToLiveAttribute("expiresIn")
                .stream(StreamViewType.NEW_AND_OLD_IMAGES)
                .partitionKey(Attribute.builder()
                        .name("PK")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("SK")
                        .build())
                .build();

        IntStream.range(1, 3).forEach(index -> {
            table.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                    .indexName("GSI-" + index)
                    .partitionKey(Attribute.builder()
                            .name("GSI-" + index + "-PK")
                            .type(AttributeType.STRING)
                            .build())
                    .sortKey(Attribute.builder()
                            .name("GSI-SK")
                            .type(AttributeType.NUMBER)
                            .build())
                    .readCapacity(1)
                    .writeCapacity(1)
                    .projectionType(ProjectionType.ALL)
                    .build());
        });

        final String serviceModule = "device-pool-service-backend";
        Function controlPlaneFunction = Function.Builder.create(this, "DeviceLabFunction")
                .handler("me.philcali.device.pool.service.DevicePools::handleRequest")
                .environment(new HashMap<String, String>() {{
                    put("TABLE_NAME", table.getTableName());
                    put("API_VERSION", "V1");
                }})
                .memorySize(512)
                .runtime(Runtime.JAVA_11)
                .timeout(Duration.seconds(30))
                .code(Code.fromAsset(String.format("../%s/target/%s-1.0-SNAPSHOT.jar", serviceModule, serviceModule)))
                .build();

        controlPlaneFunction.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "dynamodb:GetItem",
                        "dynamodb:UpdateItem",
                        "dynamodb:PutItem",
                        "dynamodb:DeleteItem",
                        "dynamodb:Query"
                ))
                .resources(Collections.singletonList(table.getTableArn()))
                .build());

        LambdaRestApi api = LambdaRestApi.Builder.create(this, "DeviceLabService")
                .handler(controlPlaneFunction)
                .endpointConfiguration(EndpointConfiguration.builder()
                        .types(Collections.singletonList(EndpointType.REGIONAL))
                        .build())
                .defaultMethodOptions(MethodOptions.builder()
                        .authorizationType(AuthorizationType.IAM)
                        .build())
                .deployOptions(StageOptions.builder()
                        .stageName("prod")
                        .build())
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowCredentials(true)
                        .allowHeaders(Arrays.asList(
                                "Authorization",
                                "Cookie",
                                "Content-Type"
                        ))
                        .allowMethods(Arrays.asList(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE"
                        ))
                        .allowOrigins(Collections.singletonList("*"))
                        .build())
                .build();

        StateMachine provisioningWorkflow = StateMachine.Builder.create(this, "ProvisioningWorkflow")
                .timeout(Duration.hours(1))
                .build();

        provisioningWorkflow.addToRolePolicy(PolicyStatement.Builder.create()
                .build());
    }
}
