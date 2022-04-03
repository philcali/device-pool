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
import me.philcali.device.pool.lab.DevicePoolLockProps;
import me.philcali.device.pool.lab.DevicePoolProps;
import me.philcali.device.pool.lab.DevicePoolType;
import me.philcali.device.pool.lab.IDevicePoolIntegration;
import me.philcali.device.pool.lab.SSMDevicePoolIntegration;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.lambda.Code;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/**
 * <p>DevicePoolExamplesInfraStack class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class DevicePoolExamplesInfraStack extends Stack {
    /**
     * <p>Constructor for DevicePoolExamplesInfraStack.</p>
     *
     * @param scope a {@link software.constructs.Construct} object
     * @param id a {@link java.lang.String} object
     */
    public DevicePoolExamplesInfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    /**
     * <p>Constructor for DevicePoolExamplesInfraStack.</p>
     *
     * @param scope a {@link software.constructs.Construct} object
     * @param id a {@link java.lang.String} object
     * @param props a {@link software.amazon.awscdk.StackProps} object
     */
    public DevicePoolExamplesInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final String version = "1.1.1";
        final String serviceModule = "device-pool-service-backend";
        final String workflowModule = "device-pool-service-events";
        final String ssmIntegrationModule = "device-pool-service-unmanaged-ssm";

        final DeviceLab deviceLab = DeviceLab.Builder.create(this, "ExampleDeviceLab")
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

        IDevicePoolIntegration integration = SSMDevicePoolIntegration.Builder.create(this)
                .locking(true)
                .code(Code.fromAsset("../../device-pool-service/device-pool-service-unmanaged/"
                        + ssmIntegrationModule + "/target/"
                        + ssmIntegrationModule + "-" + version + ".jar"))
                .build();

        deviceLab.addDevicePool(DevicePoolProps.builder()
                .integration(integration)
                .poolType(DevicePoolType.UNMANAGED)
                .name("pi-cameras")
                .lockOptions(DevicePoolLockProps.builder()
                        .enabled(true)
                        .duration(Duration.seconds(30))
                        .build())
                .build());
    }
}
