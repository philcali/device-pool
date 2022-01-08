namespace device.pool

resource DevicePool {
    identifiers: {
        poolId: String
    },
    read: GetDevicePool,
    list: ListDevicePools,
    create: CreateDevicePool,
    update: UpdateDevicePool,
    delete: DeleteDevicePool,
    resources: [Device]
}

@readonly
@http(method: "GET", uri: "/pools/{poolId}")
operation GetDevicePool {
    input: GetDevicePoolInput,
    output: GetDevicePoolOutput,
    errors: [NotFoundError, ServiceError]
}

@readonly
@paginated(items: "items")
@http(method: "GET", uri: "/pools")
operation ListDevicePools {
    input: ListDevicePoolsInput,
    output: ListDevicePoolsOutput,
    errors: [ServiceError]
}

@http(method: "POST", uri: "/pools")
operation CreateDevicePool {
    input: CreateDevicePoolInput,
    output: CreateDevicePoolOutput,
    errors: [InvalidInputError, ConflictError, ServiceError]
}

@idempotent
@http(method: "PUT", uri: "/pools/{poolId}", code: 202)
operation UpdateDevicePool {
    input: UpdateDevicePoolInput,
    output: UpdateDevicePoolOutput,
    errors: [NotFoundError, InvalidInputError, ServiceError]
}

@idempotent
@http(method: "DELETE", uri: "/pools/{poolId}", code: 204)
operation DeleteDevicePool {
    input: DeleteDevicePoolInput,
    errors: [ServiceError]
}

@input
structure GetDevicePoolInput {
    @required
    @httpLabel
    poolId: String
}

@output
structure GetDevicePoolOutput {
    @required
    name: String,
    description: String,
    @required
    type: String,
    @required
    endpoint: DevicePoolEndpoint,
    @required
    lockOptions: DevicePoolLockOptions
}

@input
structure ListDevicePoolsInput {
    @httpQuery("nextToken")
    nextToken: String,
    @httpQuery("limit")
    limit: Integer
}

@output
structure ListDevicePoolsOutput {
    nextToken: String,
    @required
    items: DevicePoolList
}

list DevicePoolList {
    member: DevicePoolSummary
}

@references([{resource: DevicePool}])
structure DevicePoolSummary {
    @required
    poolId: String,
    @required
    name: String,
    @required
    type: String
}

structure DevicePoolEndpoint {
    @required
    uri: String,
    @required
    type: String
}

structure DevicePoolLockOptions {
    @required
    enabled: Boolean,
    duration: Integer
}

@input
structure CreateDevicePoolInput {
    @required
    name: String,
    @required
    type: String,
    @required
    endpoint: DevicePoolEndpoint,
    description: String,
    lockOptions: DevicePoolLockOptions
}

@output
structure CreateDevicePoolOutput {
    @required
    poolId: String,
    @required
    name: String,
    @required
    type: String,
    @required
    endpoint: DevicePoolEndpoint,
    description: String,
    lockOptions: DevicePoolLockOptions
}

@input
structure UpdateDevicePoolInput {
    @required
    @httpLabel
    poolId: String,
    description: String,
    endpoint: DevicePoolEndpoint,
    lockOptions: DevicePoolLockOptions
}

@output
structure UpdateDevicePoolOutput {
    @required
    poolId: String,
    description: String,
    @required
    endpoint: DevicePoolEndpoint,
    @required
    lockOptions: DevicePoolLockOptions
}

@input
structure DeleteDevicePoolInput {
    @required
    @httpLabel
    poolId: String
}