/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

import java.util.HashMap;

class DevicePoolInfrastructureTest {

    @Test
    void GIVEN_infra_stack_is_created_WHEN_app_is_synthesized_THEN_resources_are_created() {
        App app = new App();
        DevicePoolInfrastructureStack stack = new DevicePoolInfrastructureStack(app, "test", StackProps.builder()
                .build());

        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::DynamoDB::Table", new HashMap<String, String>() {{
            put("TableName", "DeviceLab");
        }});
    }
}
