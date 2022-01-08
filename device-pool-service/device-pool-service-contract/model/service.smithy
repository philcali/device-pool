namespace device.pool

use aws.auth#sigv4
use aws.protocols#restJson1

@restJson1
@sigv4(name: "execute-api")
@paginated(
    inputToken: "nextToken",
    outputToken: "nextToken",
    pageSize: "limit"
)
service DeviceLab {
    version: "2022-01-08",
    resources: [DevicePool, Provision]
}
