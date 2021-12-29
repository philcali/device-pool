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
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSourceProps;
import software.amazon.awscdk.services.stepfunctions.CatchProps;
import software.amazon.awscdk.services.stepfunctions.Choice;
import software.amazon.awscdk.services.stepfunctions.Condition;
import software.amazon.awscdk.services.stepfunctions.IChainable;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.Wait;
import software.amazon.awscdk.services.stepfunctions.WaitProps;
import software.amazon.awscdk.services.stepfunctions.WaitTime;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DevicePoolInfrastructureStack extends Stack {
    private static final String VERSION = "1.0-SNAPSHOT";

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

        final PolicyStatement databaseInteractionPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "dynamodb:GetItem",
                        "dynamodb:UpdateItem",
                        "dynamodb:PutItem",
                        "dynamodb:DeleteItem",
                        "dynamodb:Query"
                ))
                .resources(Collections.singletonList(table.getTableArn()))
                .build();

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
                .code(Code.fromAsset(String.format("../%s/target/%s-%s.jar", serviceModule, serviceModule, VERSION)))
                .build();
        controlPlaneFunction.addToRolePolicy(databaseInteractionPolicy);

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

        final String eventsModule = "device-pool-service-events";
        final Code event = Code.fromAsset(String.format("../%s/target/%s-%s.jar", eventsModule, eventsModule, VERSION));
        final List<String> lambdaSteps = Arrays.asList(
                "startProvision",
                "createReservation",
                "obtainDevices",
                "failProvision",
                "finishProvision"
        );
        final Map<String, LambdaInvoke> invokeSteps = lambdaSteps.stream()
                .collect(Collectors.toMap(
                        java.util.function.Function.identity(),
                        stepName -> {
                            Function stepFunction = Function.Builder.create(this, stepName + "Function")
                                    .handler("me.philcali.device.pool.service.DevicePoolEvents::" + stepName + "Step")
                                    .environment(new HashMap<String, String>() {{
                                        put("TABLE_NAME", table.getTableName());
                                    }})
                                    .memorySize(512)
                                    .runtime(Runtime.JAVA_11)
                                    .timeout(Duration.minutes(5))
                                    .code(event)
                                    .build();
                            stepFunction.addToRolePolicy(databaseInteractionPolicy);

                            return LambdaInvoke.Builder.create(this, stepName + "Step")
                                    .lambdaFunction(stepFunction)
                                    .retryOnServiceExceptions(true)
                                    .build();
                        }));

        // Attach catch to all invoke steps
        invokeSteps.entrySet().stream()
                .filter(e -> !e.getKey().equals("failProvision"))
                .forEach(entry -> entry.getValue().addCatch(invokeSteps.get("failProvision")));

        Wait waitTime = new Wait(this, "waitLoop", WaitProps.builder()
                .time(WaitTime.duration(Duration.seconds(5)))
                .build());

        // Scale loop for unmanaged pools
        IChainable scaleLoop = invokeSteps.get("obtainDevices")
                .next(new Choice(this, "Is Done?")
                        .when(Condition.booleanEquals("$.done", true), invokeSteps.get("finishProvision"))
                        .otherwise(waitTime.next(invokeSteps.get("obtainDevices"))));

        // Entire definition
        IChainable definition = new Choice(this, "Is Managed?")
                .when(Condition.stringEquals("$.type", "UNMANAGED"), scaleLoop)
                .otherwise(invokeSteps.get("createReservation"));

        StateMachine provisioningWorkflow = StateMachine.Builder.create(this, "ProvisioningWorkflow")
                .definition(definition)
                .timeout(Duration.hours(1))
                .build();

        Function eventsFunction = Function.Builder.create(this, "DeviceLabEvents")
                .handler("me.philcali.device.pool.service.DevicePoolEvents::handleProvisionCreation")
                .environment(new HashMap<String, String>() {{
                    put("WORKFLOW_ID", provisioningWorkflow.getStateMachineArn());
                }})
                .memorySize(512)
                .timeout(Duration.minutes(5))
                .runtime(Runtime.JAVA_11)
                .code(event)
                .build();

        eventsFunction.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Collections.singletonList("states:StartExecution"))
                .resources(Collections.singletonList(provisioningWorkflow.getStateMachineArn()))
                .build());

        eventsFunction.addEventSource(new DynamoEventSource(table, DynamoEventSourceProps.builder()
                .enabled(true)
                .batchSize(100)
                .startingPosition(StartingPosition.TRIM_HORIZON)
                .build()));
    }
}
