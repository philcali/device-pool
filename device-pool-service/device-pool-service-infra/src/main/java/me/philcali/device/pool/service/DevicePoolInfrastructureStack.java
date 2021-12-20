package me.philcali.device.pool.service;

import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;

import java.util.stream.IntStream;

public class DevicePoolInfrastructureStack extends Stack {

    public DevicePoolInfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table table = Table.Builder.create(this, "DevicePoolTable")
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(1)
                .writeCapacity(1)
                .tableName("DeviceLab")
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
    }
}
