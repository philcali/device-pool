package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.model.CreateDevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
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
public class Pools extends RepositoryResource<DevicePoolObject, CreateDevicePoolObject, UpdateDevicePoolObject> {
    static final String ID = "poolId";

    @Inject
    public Pools(DevicePoolRepo poolRepo) {
        super(poolRepo);
    }

    @GET
    @Path("/{" + ID + "}")
    public Response get(@Context SecurityContext context, @PathParam(ID) String poolId) {
        return getItem(context, poolId);
    }

    @Path("/{" + ID + "}/devices")
    public Class<Devices> devices() {
        return Devices.class;
    }

    @Path("/{" + ID + "}/provisions")
    public Class<Provisions> provisions() {
        return Provisions.class;
    }

    @GET
    public Response list(
            @Context SecurityContext context,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        return listItems(context, limit, nextToken);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext context, CreateDevicePoolObject input) {
        return createItem(context, input);
    }

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(
            @Context SecurityContext context,
            @PathParam(ID) String poolId,
            UpdateDevicePoolObject input) {
        return updateItem(context, UpdateDevicePoolObject.builder().from(input).name(poolId).build());
    }

    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(@Context SecurityContext context, @PathParam(ID) String poolId) {
        return deleteItem(context, poolId);
    }
}
