# Device Lab CLI

Script control plane interaction with this CLI tool. The `endpoint` is a required parameter.

## Installation

```shell
curl https://raw.githubusercontent.com/philcali/device-pool/main/device-pool-cli/scripts/install.sh | sh
```

## Usage

```shell
Usage: device-lab [-v] --endpoint=<endpoint> [COMMAND]
Device Lab CLI for the control plane
      --endpoint=<endpoint>
                  Endpoint override
  -v, --verbose   Verbose flag to output wire details
Commands:
  help                      Displays help information about the specified
                              command
  devices                   Device Lab CLI for the data plane
  cancel-provision          Cancel a single non-terminal provision request
  cancel-reservation        Cancels a single non-terminal device reservation
  create-device             Creates a single device for a device pool
  create-device-lock        Creates a lock on a single device
  create-device-pool        Creates a single device pool
  create-device-pool-lock   Creates a lock on a single device pool
  create-provision          Creates a single provision request
  delete-device             Deletes a single device on a device pool
  delete-device-pool        Deletes a single device pool and all associated data
  delete-provision          Deletes a single terminal provision request
  extend-device-lock        Extends a lock on a single device
  extend-device-pool-lock   Extends a lock on a single device pool
  get-device                Obtains a single device metadata
  get-device-lock           Obtains lock metadata on a single device
  get-device-pool           Obtains a single device pool metadata
  get-device-pool-lock      Obtains lock metadata on a single pool
  get-provision             Obtains a single provision request metadata
  get-reservation           Obtains a single device reservation
  list-device-pools         List device pools
  list-devices              List devices to device pools
  list-provisions           List provision requests to device pools
  list-reservations         List device reservation requests to provisions
  release-device-lock       Forcibly releases a lock held on a single device
  release-device-pool-lock  Forcibly releases a lock held on a single pool
  update-device             Updates a single device metadata record
  update-device-pool        Updates a single device pool record
```

## Data plane Usage

The purpose of the `devices` subcommand is to exercise data plane control of devices being provisioned.
This gives basic command access to `execute` and `cp` for file transfer.

```shell
Usage: device-lab devices [-v] [--all] [--use-ssm] [--amount=<amount>]
                          --endpoint=<endpoint> -p=<platform> [-P=<port>]
                          --pool-id=<poolId> [-pt=<provisionTimeout>]
                          [--s3-bucket=<bucketName>] [-u=<userName>] [COMMAND]
Device Lab CLI for the data plane
      --all                reserve all devices for this operation
      --amount=<amount>    amount of devices in the pool to reserve
      --endpoint=<endpoint>
                           Endpoint override
  -p, --platform=<platform>
                           target platform of the device in form of 'os:arch',
                             eg: 'linux:armv6'
  -P, --port=<port>        port of the SSH client connection
      --pool-id=<poolId>   name of the device pool
      -pt, --provision-timeout=<provisionTimeout>
                           timeout waiting for the operation in seconds
      --s3-bucket=<bucketName>
                           name of the s3 bucket for file transfer
  -u, --user=<userName>    user for the SSH host
      --use-ssm            communicate connections using SSM
  -v, --verbose            Verbose flag to output wire details
Commands:
  cp       Copies files to and from the local machine
  execute  Runs an arbitrary command on the devices
```

If you want to try this CLI on infra you own, go to [device-pool-examples-infra][1] and follow the
instructions.

[1]: ../device-pool-examples/device-pool-examples-infra/README.md