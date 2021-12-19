package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.annotation.RequiresAuth;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Singleton
@Path("/pools")
public class Pools {
    private static final String ID = "poolId";
    private final DevicePoolRepo poolRepo;

    @Inject
    public Pools(DevicePoolRepo poolRepo) {
        this.poolRepo = poolRepo;
    }

    @GET
    @RequiresAuth
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context SecurityContext context, @PathParam(ID) String poolId) {
        return Response.ok(poolRepo.get(CompositeKey.of(context.getUserPrincipal().getName()), poolId)).build();
    }

    @GET
    @RequiresAuth
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @Context SecurityContext context,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        if (limit <= 0 || limit > DevicePoolRepo.MAX_ITEMS) {
            limit = DevicePoolRepo.MAX_ITEMS;
        }
        QueryResults<DevicePoolObject> objects = poolRepo.list(CompositeKey.of(context.getUserPrincipal().getName()),
                QueryParams.builder()
                        .limit(limit)
                        .nextToken(nextToken)
                        .build());
        return Response.ok(objects).build();
    }

    @POST
    @RequiresAuth
    public Response create(@Context SecurityContext context) {
        return Response.ok().build();
    }

    @DELETE
    @RequiresAuth
    @Path("/{" + ID + "}")
    public Response delete(@Context SecurityContext context, @PathParam(ID) String poolId) {
        poolRepo.delete(CompositeKey.of(context.getUserPrincipal().getName()), poolId);
        return Response.noContent().build();
    }
}
