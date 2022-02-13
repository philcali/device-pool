# Examples

This module contains examples to facilitate hands-on style learning for
using the device pool libraries. Using the example application:

```shell
Usage: device-pool-examples [-h] [COMMAND]
Local app containing examples to show case device pools.
  -h, --help   Displays help usage information
Commands:
  ec2    Demonstrates autoscaling and ec2 device pools
  lab    Interact with the device-lab control plane
  local  An example app that uses a local static pool to provision
```

## Local Provisioning

```shell
Usage: device-pool-examples local [-h] [-p]... [-P=<port>] [-u=<userName>]
                                  -n=<hostNames> [-n=<hostNames>]... [COMMAND]
An example app that uses a local static pool to provision
  -h, --help               Displays help usage information
  -n, --host=<hostNames>   IP addresses of the hosts representing this pool
  -p, --password           SSH password for the selected hosts
  -P, --port=<port>        SSH port to use
  -u, --user=<userName>    SSH username for the selected hosts
Commands:
  exec  Runs an arbitrary command on the devices
  cp    Copies files to and from devices
```

__Executing Commands__

```shell
> local -u pi -p -n 192.168.1.206 exec "echo Hello World"
Output from host-0
Hello World
```

## EC2 Provisioning

```shell
Usage: device-pool-examples ec2 [-h] -g=<groupName> [-i=<pemFile>]
                                -p=<platformOS>
Demonstrates autoscaling and ec2 device pools
  -g, --group=<groupName>    AutoScaling group name to back provisioning
  -h, --help                 Displays help usage information
  -i, --identity=<pemFile>
  -p, --platform=<platformOS>
                             Platform / OS combo in the form of 'os:arch', eg:
                               'unix:armv7'
```

## Provision with control plane

```shell
Usage: device-pool-examples lab [-h] [-d=<poolName>] --endpoint=<endpoint>
                                [COMMAND]
Interact with the device-lab control plane
  -d, --name=<poolName>   Name of the device pool
      --endpoint=<endpoint>
                          Override for device lab endpoint
  -h, --help              Displays help usage information
Commands:
  init       Sets up a managed device pool with devices
  provision  Creates a provision on a pool and demonstrates acquisition
  destroy    Destroys a device pool through the control plane
```

1. Setup custom provisioning with CDK (see [README][1] for instructions)
2. Invoke with the `init` and then `provision` to see it work

[1]: device-pool-examples-infra/README.md