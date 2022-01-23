# Simplified Device Pools

[![codecov](https://codecov.io/gh/philcali/device-pool/branch/main/graph/badge.svg?token=WIIU9GHW69)](https://codecov.io/gh/philcali/device-pool)
[![Java CI with Maven](https://github.com/philcali/device-pool/actions/workflows/maven.yml/badge.svg)](https://github.com/philcali/device-pool/actions/workflows/maven.yml)

This collection of software provides an end-to-end solution for provisioning, obtaining,
and interacting with devices from a device pool.

## What is a Device Pool?

A `DevicePool` is an abstraction that represents a category, collection,
or catalog for a particular type of device. The key action on
a `DevicePool` is to provision a `Device`. What does that even mean?
In API terms, the `DevicePool` is the control-plane entrypoint for
device resources, and the `Device` resource is the data-plane.

In the IoT world, the device pool (aka device labs), are commonly used
for quality assurance purposes. Where possible, pools can represent
different operating system and CPU architectures computers.

## What is a Device?

A `Device` is an abstraction for interacting with computers, either
physical or virtual. Physical or virtual? Indeed, where virtual
computers can be leased on many of the cloud provisioning systems
on demand (elastically) or physical if a lab is owned on-premise.

## What is in this code-base?

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
provisioning, reservations, and interacting with devices. The modules are then:

- `device-pool-ec2`: provisioning on EC2 using autoscaling as the provisioning force.
- `device-pool-s3`: content transfer over S3 (files to and from).
- `device-pool-ssm`: execute commands over SSM `RunDocument`s.
- `device-pool-ssh`: execute commands over SSH and SCP files transfer.
- `device-pool-ddb`: provides a distributed lock to be used for locking devices or pools.
- `device-pool-client`: provides an abstraction over a customized `DeviceLab` control-plane.

## What is the DeviceLab control-plane?

The largest part of the code-base can be found in the children modules of `device-pool-service`.
The `DeviceLab` abstracts the `DevicePool` resource which instructs the service how provisioning
is performed, namely through the `MANAGED` and `UNMANAGED` types.

- `device-pool-service-backend`: Jersey Lambda container that handles the endpoints.
- `device-pool-service-events`: Event Lambda handling provisioning workflow steps.
- `device-pool-service-infra`: Java based CDK that creates the database and endpoints for the service.

The rest of the modules support the former, introducing contracts, database implementation and
other advanced extension points.