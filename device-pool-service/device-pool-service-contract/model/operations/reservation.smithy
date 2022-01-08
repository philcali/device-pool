namespace device.pool

resource Reservation {
    identifiers: {
        poolId: String,
        provisionId: String,
        reservationId: String
    },
    read: GetReservation,
    list: ListReservations
}

@readonly
@http(method: "GET", uri: "/pools/{poolId}/provisions/{provisionId}/reservations/{reservationId}")
operation GetReservation {
    input: GetReservationInput,
    output: GetReservationOutput,
    errors: [NotFoundError, ServiceError]
}

@readonly
@paginated(items: "items")
@http(method: "GET", uri: "/pools/{poolId}/provisions/{provisionId}/reservations")
operation ListReservations {
    input: ListReservationsInput,
    output: ListReservationsOutput,
    errors: [NotFoundError, ServiceError]
}

@input
structure GetReservationInput {
    @required
    @httpLabel
    poolId: String,
    @required
    @httpLabel
    provisionId: String,
    @required
    @httpLabel
    reservationId: String
}

@output
@references([
    {resource: DevicePool},
    {resource: Device}
])
structure GetReservationOutput {
    @required
    poolId: String,
    @required
    deviceId: String,
    @required
    status: String
}

@input
structure ListReservationsInput {
    @httpQuery("nextToken")
    nextToken: String,
    @httpQuery("limit")
    limit: Integer,
    @required
    @httpLabel
    poolId: String,
    @required
    @httpLabel
    provisionId: String
}

@output
structure ListReservationsOutput {
    nextToken: String,
    @required
    items: ReservationList
}

list ReservationList {
    member: ReservationSummary
}

@references([
    {resource: DevicePool},
    {resource: Provision},
    {resource: Reservation},
    {resource: Device}
])
structure ReservationSummary {
    @required
    poolId: String,
    @required
    provisionId: String,
    @required
    reservationId: String,
    @required
    deviceId: String,
    @required
    status: String
}