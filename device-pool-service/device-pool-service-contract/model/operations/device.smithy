namespace device.pool

resource Device {
    identifiers: {
        poolId: String,
        deviceId: String
    },
    read: GetDevice,
    list: ListDevices
}

@readonly
@http(method: "GET", uri: "/pools/{poolId}/devices/{deviceId}")
operation GetDevice {
    input: GetDeviceInput,
    output: GetDeviceOutput,
    errors: [NotFoundError, ServiceError]
}

@readonly
@paginated(items: "items")
@http(method: "GET", uri: "/pools/{poolId}/devices")
operation ListDevices {
    input: ListDevicesInput,
    output: ListDevicesOutput,
    errors: [NotFoundError, ServiceError]
}


@input
structure GetDeviceInput {
    @required
    @httpLabel
    poolId: String,
    @required
    @httpLabel
    deviceId: String
}

@output
@references([{resource: DevicePool}])
structure GetDeviceOutput {
    @required
    poolId: String,
    @required
    deviceId: String,
    @required
    publicAddress: String,
    privateAddress: String
}

@input
structure ListDevicesInput {
    @httpQuery("nextToken")
    nextToken: String,
    @httpQuery("limit")
    limit: Integer,
    @required
    @httpLabel
    poolId: String
}

@output
structure ListDevicesOutput {
    nextToken: String,
    @required
    items: DeviceList
}

list DeviceList {
    member: DeviceSummary
}

@references([{resource: DevicePool}, {resource: Device}])
structure DeviceSummary {
    @required
    poolId: String,
    @required
    deviceId: String,
    @required
    publicAddress: String,
    privateAddress: String
}
