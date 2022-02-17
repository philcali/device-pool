/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * <p>CatchAllExceptionMapper class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<RuntimeException> {
    /** {@inheritDoc} */
    @Override
    public Response toResponse(RuntimeException error) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"" + error.getMessage() + "\"}")
                .build();
    }
}
