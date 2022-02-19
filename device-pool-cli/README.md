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

If you want to try this CLI on infra you own, go to [device-pool-examples-infra][1] and follow the
instructions.

[1]: ../device-pool-examples/device-pool-examples-infra/README.md