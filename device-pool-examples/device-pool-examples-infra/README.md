# Example Device Lab Infra

This is a very simple device lab infra created through construct found in [@philcali-cdk/device-lab][1].

[1]: https://github.com/philcali/philcali-cdk/tree/master/device-lab

## Initial Setup

1. Install [AWS CDK][2] if you don't already have it
2. Clone this repository
3. Build it
```shell
mvn -pl device-pool-examples/device-pool-examples-infra -am clean package
```
4. Deploy it
```shell
cd device-pool-examples/device-pool-examples-infra
cdk deploy
```

[2]: https://docs.aws.amazon.com/cdk/v2/guide/home.html

Once the deployment ends, it'll output the control plane base URL which you will need to
exercise the [device-pool-examples][3] or [device-pool-cli][4]. Some `DevicePool` as infrastructure
are demonstrated in the example:

- `SSMDevicePoolIntegration`: creates an UNMANAGED device pool that is connected to SSM
- `IotDevicePoolIntegration`: creates an UNMANAGED device pool that is connected to IoT

[3]: ../README.md
[4]: ../../device-pool-cli/README.md

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

Enjoy!
