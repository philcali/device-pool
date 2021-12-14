package me.philcali.device.pool.service.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/echo")
public class Echo {
    @GET
    @Path("/{path}")
    public Response message(@PathParam("path") String path) {
        return Response.ok(path).build();
    }
}
