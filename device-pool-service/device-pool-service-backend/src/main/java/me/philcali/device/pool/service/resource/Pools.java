package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDevicePoolObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
@Produces(MediaType.APPLICATION_JSON)
public class Pools {
    private static final String ID = "poolId";
    private final DevicePoolRepo poolRepo;

    @Inject
    public Pools(DevicePoolRepo poolRepo) {
        this.poolRepo = poolRepo;
    }

    @GET
    @Path("/{" + ID + "}")
    public Response get(@Context SecurityContext context, @PathParam(ID) String poolId) {
        return Response.ok(poolRepo.get(CompositeKey.of(context.getUserPrincipal().getName()), poolId)).build();
    }

    @GET
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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext context, CreateDevicePoolObject input) {
        return Response.ok(poolRepo.create(CompositeKey.of(context.getUserPrincipal().getName()), input)).build();
    }

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(
            @Context SecurityContext context,
            @PathParam(ID) String poolId,
            UpdateDevicePoolObject input) {
        CompositeKey account = CompositeKey.of(context.getUserPrincipal().getName());
        return Response.accepted(poolRepo.update(account, b -> b.from(input).name(poolId))).build();
    }

    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(@Context SecurityContext context, @PathParam(ID) String poolId) {
        poolRepo.delete(CompositeKey.of(context.getUserPrincipal().getName()), poolId);
        return Response.noContent().build();
    }
}
