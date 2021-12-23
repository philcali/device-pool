package me.philcali.device.pool.service.resource;


import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UpdateDeviceObject;

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
public class Devices {
    private static final String ID = "deviceId";
    private final DevicePoolRepo pools;
    private final DeviceRepo devices;

    @Inject
    public Devices(final DevicePoolRepo pools, final DeviceRepo devices) {
        this.pools = pools;
        this.devices = devices;
    }

    private CompositeKey toKey(SecurityContext context, String poolId) {
        return CompositeKey.builder()
                .from(pools.get(CompositeKey.of(context.getUserPrincipal().getName()), poolId).key())
                .addResources(poolId)
                .build();
    }

    @GET
    @Path("/{" + ID + "}")
    public Response get(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String deviceId) {
        return Response.ok(devices.get(toKey(context, poolId), deviceId)).build();
    }

    @GET
    public Response list(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        if (limit <= 0 || limit > DeviceRepo.MAX_RESULTS) {
            limit = DeviceRepo.MAX_RESULTS;
        }
        QueryResults<DeviceObject> results = devices.list(toKey(context, poolId), QueryParams.builder()
                .limit(limit)
                .nextToken(nextToken)
                .build());
        return Response.ok(results).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            CreateDeviceObject create) {
        return Response.accepted(devices.create(toKey(context, poolId), create)).build();
    }

    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String deviceId) {
        devices.delete(toKey(context, poolId), deviceId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{" + ID + "}")
    public Response update(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String deviceId,
            UpdateDeviceObject update) {
        return Response.ok(devices.update(toKey(context, poolId), UpdateDeviceObject.builder()
                .from(update)
                .id(deviceId)
                .build()))
                .build();
    }
}
