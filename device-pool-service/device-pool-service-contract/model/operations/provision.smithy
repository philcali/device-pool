namespace device.pool

resource Provision {
    identifiers: {
        poolId: String,
        provisionId: String
    },
    read: GetProvision,
    list: ListProvisions,
    resources: [Reservation]
}

@readonly
@http(method: "GET", uri: "/pools/{poolId}/provisions/{provisionId}")
operation GetProvision {
    input: GetProvisionInput,
    output: GetProvisionOutput,
    errors: [NotFoundError, ServiceError]
}

@readonly
@paginated(items: "items")
@http(method: "GET", uri: "/pools/{poolId}/provisions")
operation ListProvisions {
    input: ListProvisionsInput,
    output: ListProvisionsOutput,
    errors: [NotFoundError, ServiceError]
}

@input
structure GetProvisionInput {
    @required
    @httpLabel
    poolId: String,
    @required
    @httpLabel
    provisionId: String
}

@output
@references([{resource: DevicePool}])
structure GetProvisionOutput {
    @required
    poolId: String,
    @required
    amount: Integer,
    @required
    status: String
}

@input
structure ListProvisionsInput {
    @httpQuery("nextToken")
    nextToken: String,
    @httpQuery("limit")
    limit: Integer,
    @required
    @httpLabel
    poolId: String
}

@output
structure ListProvisionsOutput {
    nextToken: String,
    @required
    items: ProvisionList
}

list ProvisionList {
    member: ProvisionSummary
}

@references([
    {resource: DevicePool},
    {resource: Provision}
])
structure ProvisionSummary {
    @required
    poolId: String,
    @required
    provisionId: String,
    @required
    status: String
}