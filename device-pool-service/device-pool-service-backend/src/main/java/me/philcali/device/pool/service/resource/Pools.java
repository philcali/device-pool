package me.philcali.device.pool.service.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/pools")
public class Pools {
    private static final String ID = "poolId";

    @GET
    @Path("/{" + ID + "}")
    public Response get(@PathParam(ID) String poolId) {
        return Response.ok().build();
    }

    @GET
    public Response list(
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        return Response.ok().build();
    }

    @POST
    public Response create() {
        return Response.ok().build();
    }

    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(@PathParam(ID) String poolId) {
        return Response.noContent().build();
    }
}
