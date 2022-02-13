/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.examples.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

public class DevicePoolExamplesInfraTest {

     @Test
     public void testStack() {
         App app = new App();
         DevicePoolExamplesInfraStack stack = new DevicePoolExamplesInfraStack(app, "test");

         Template template = Template.fromStack(stack);

         template.hasResourceProperties("AWS::DynamoDB::Table", new HashMap<String, String>() {{
           put("TableName", "ExampleDeviceLabTable");
         }});
     }
}
