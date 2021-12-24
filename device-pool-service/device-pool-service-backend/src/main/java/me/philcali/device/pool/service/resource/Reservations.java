package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;

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
public class Reservations
        extends RepositoryResource<ReservationObject, CreateReservationObject, UpdateReservationObject> {
    static final String ID = "reservationId";

    @Inject
    public Reservations(
            ReservationRepo repository,
            DevicePoolRepo pools,
            ProvisionRepo provisions) {
        super(repository, pools, provisions);
    }

    @GET
    public Response list(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Provisions.ID) String provisionId,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        return listItems(context, limit, nextToken, poolId, provisionId);
    }

    @GET
    @Path("/{" + ID + "}")
    public Response get(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Provisions.ID) String provisionId,
            @PathParam(ID) String reservationId) {
        return getItem(context, reservationId, poolId, provisionId);
    }

    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Provisions.ID) String provisionId,
            @PathParam(ID) String reservationId) {
        return deleteItem(context, reservationId, poolId, provisionId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Provisions.ID) String provisionId,
            CreateReservationObject create) {
        return createItem(context, create, poolId, provisionId);
    }

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Provisions.ID) String provisionId,
            @PathParam(ID) String reservationId,
            UpdateReservationObject update) {
        UpdateReservationObject newUpdate = UpdateReservationObject.builder()
                .from(update)
                .id(reservationId)
                .build();
        return updateItem(context, newUpdate, poolId, provisionId);
    }
}
