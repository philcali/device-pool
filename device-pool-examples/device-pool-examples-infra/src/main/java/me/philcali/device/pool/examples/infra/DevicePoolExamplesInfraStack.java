/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.examples.infra;

import me.philcali.device.pool.lab.DeviceLab;
import me.philcali.device.pool.lab.DeviceLabApiProps;
import me.philcali.device.pool.lab.DeviceLabTableProps;
import me.philcali.device.pool.lab.DeviceLabWorkflowProps;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.lambda.Code;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class DevicePoolExamplesInfraStack extends Stack {
    public DevicePoolExamplesInfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DevicePoolExamplesInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final String version = "1.0-SNAPSHOT";
        final String serviceModule = "device-pool-service-backend";
        final String workflowModule = "device-pool-service-events";

        DeviceLab.Builder.create(this, "ExampleDeviceLab")
                .tableProps(DeviceLabTableProps.builder()
                        .billingMode(BillingMode.PROVISIONED)
                        .readCapacity(1)
                        .writeCapacity(1)
                        .tableName("ExampleDeviceLabTable")
                        .build())
                .apiProps(DeviceLabApiProps.builder()
                        .apiName("ExampleDeviceLabAPI")
                        .build())
                .workflowProps(DeviceLabWorkflowProps.builder()
                        .workflowName("ExampleDeviceLabWorkflow")
                        .build())
                .serviceCode(Code.fromAsset("../../device-pool-service/"
                        + serviceModule + "/target/"
                        + serviceModule + "-" + version + ".jar"))
                .workflowCode(Code.fromAsset("../../device-pool-service/"
                        + workflowModule + "/target/"
                        + workflowModule + "-" + version + ".jar"))
                .build();
    }
}
