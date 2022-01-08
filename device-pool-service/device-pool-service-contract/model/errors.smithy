namespace device.pool

@error("client")
@httpError(404)
structure NotFoundError {
    @required
    resourceType: String
}

@error("client")
@httpError(409)
structure ConflictError {
    @required
    resourceType: String
}

@error("client")
structure InvalidInputError {
    @required
    resourceType: String,
    @required
    message: String
}

@error("server")
structure ServiceError {
}