package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;

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
@Produces(MediaType.APPLICATION_JSON)
public class Provisions extends RepositoryResource<ProvisionObject, CreateProvisionObject, UpdateProvisionObject> {
    static final String ID = "provisionId";

    @Inject
    public Provisions(ProvisionRepo provisions, DevicePoolRepo pools) {
        super(provisions, pools);
    }

    @Path("/{" + ID + "}/reservations")
    public Class<Reservations> reservations() {
        return Reservations.class;
    }

    @GET
    public Response list(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        return listItems(context, limit, nextToken, poolId);
    }

    @GET
    @Path("/{" + ID + "}")
    public Response get(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String provisionId) {
        return getItem(context, provisionId, poolId);
    }

    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String provisionId) {
        return deleteItem(context, provisionId, poolId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            CreateProvisionObject create) {
        return createItem(context, create, poolId);
    }

    @POST
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cancel(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String provisionId) {
        UpdateProvisionObject update = UpdateProvisionObject.builder()
                .id(provisionId)
                .status(Status.CANCELING)
                .build();
        return updateItem(context, update, poolId);
    }
}
