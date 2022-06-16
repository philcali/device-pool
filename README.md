# Simplified Device Pools

[![Java CI with Maven](https://github.com/philcali/device-pool/actions/workflows/maven.yml/badge.svg)](https://github.com/philcali/device-pool/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/philcali/device-pool/branch/main/graph/badge.svg?token=WIIU9GHW69)](https://codecov.io/gh/philcali/device-pool)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fphilcali%2Fdevice-pool.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fphilcali%2Fdevice-pool?ref=badge_shield)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/philcali/device-pool.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/philcali/device-pool/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/philcali/device-pool.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/philcali/device-pool/context:java)

This collection of software provides an end-to-end solution for provisioning, obtaining,
and interacting with devices from a device pool for any form of automated device interaction, but
especially geared towards quality assurance automation. Here's a basic example:

```java
DevicePool devicePool = DevicePool.create();
List<Device> devices = devicePool.provisionSync(5, 1, TimeUnit.MINUTES);
// Execute a command on the device
devices.forEach(device -> {
    CommandOutput output = device.execute(CommandInput.of("echo Hello World"));
    System.out.println(device.id() + ": " + output.toUTF8String());
});
```

Interested in more examples? Head over to [the examples](device-pool-examples).

## Installation Instructions

All snapshots and releases are delivered to `artifacts.philcali.me`
presently. Until the libraries exist in Maven central,
you must inform your build tool to use the
repository like below:

```xml
<repositories>
    <repository>
        <url>https://artifacts.philcali.me/maven/release</url>
        <id>philcali-maven-releases</id>
    </repository>
</repositories>
```

Then the following in your dependency closure.

```xml
<dependency>
    <groupId>me.philcali</groupId>
    <artifactId>device-pool-api</artifactId>
    <version>${philcali.device.version}</version>
</dependency>
```

Where `${philcali.device.version}` matches the git tag
matching the release. Interested in customizing the client library?
Pull in [the client side modules](#client-side-modules) as
necessary.

## What is a Device Pool?

A `DevicePool` is an abstraction that represents a category, collection,
or catalog for a particular type of device. The key action on
a `DevicePool` is to provision a `Device`. What does that even mean?
In API terms, the `DevicePool` is the control plane entrypoint for
device resources, and the `Device` resource is the data plane.

In the IoT world, the device pool (aka device labs), are commonly used
for quality assurance purposes. Where possible, pools can represent
different operating system and CPU architectures computers.

## What is a Device?

A `Device` is an abstraction for interacting with computers, either
physical or virtual. Physical or virtual? Indeed, where virtual
computers can be leased on many of the cloud provisioning systems
on demand (elastically) or physical if a lab is owned on-premise.

## What is in this code base?

The best way to understand and unwrap the layers in this project
is to start from the `device-pool-api` module. The contract is defined
from the client perspective:

- The `DevicePool`: provision, obtain, and release `Device`s.
- The `Device`: with some execution and file transfer capabilities.

These core primitives allow the programmatic interaction described above. Understanding
that these are useful abstractions, they are far too high level. The same package introduces
a `Base` derivative of the aforementioned primitives, with a couple of new extension points:

- `ProvisionService` and `ReservationService` for a `BaseDevicePool`
- `ConnectionFactory` and `ContentAgentTransferFactory` for a `BaseDevice`

These mid-level abstractions are more useful, and facilitates other useful decorations
explained later. Many of the surrounding modules then implement these to allow flexible
provisioning, reservations, and interacting with devices.

## Client Side Modules

A client library developing against a `DevicePool` and `Device` can
use any of the following modules as necessary.

- `device-pool-ec2`: provisioning on EC2 using autoscaling as the provisioning force.
- `device-pool-s3`: content transfer over S3 (files to and from).
- `device-pool-ssm`: execute commands over SSM `RunDocument`s.
- `device-pool-iot`: execute commands over MQTT and AWS IoT Device Shadow.
- `device-pool-ssh`: execute commands over SSH and SCP files transfer.
- `device-pool-ddb`: provides a distributed lock to be used for locking devices or pools.
- `device-pool-client`: provides an abstraction over a customized `DeviceLab` control plane.

## What is the DeviceLab control plane?

The largest part of the code base can be found in the children modules of `device-pool-service`.
The `DeviceLab` abstracts the `DevicePool` resource which instructs the service how provisioning
is performed, namely through the `MANAGED` and `UNMANAGED` types.

- `device-pool-service-backend`: Jersey Lambda container that handles the endpoints.
- `device-pool-service-events`: Event Lambda handling provisioning workflow steps.

The rest of the modules support the former, introducing contracts, database implementation and
other advanced extension points.

Interested in setting up your own control plane? Super easy. Check out the infrastructure [setup instructions][2] or
take a peek at the [infrastructure example][3] within this repo.

[![Architecture][4]][4]

[2]: https://github.com/philcali/philcali-cdk/tree/master/device-lab
[3]: device-pool-examples/device-pool-examples-infra/README.md
[4]: images/DevicePool-DeviceLab.png


## Examples

Interested in spinning up some cheap infrastructure or
see more ways to provision and communicate to devices? Head
over to [the examples](device-pool-examples).

## Recipes

Check out [the recipes](recipes/README.md) for all kinds of integration snippets.

## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fphilcali%2Fdevice-pool.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fphilcali%2Fdevice-pool?ref=badge_large)
