/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.resource;


import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.model.CreateDeviceObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
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

/**
 * <p>Devices class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class Devices extends RepositoryResource<DeviceObject, CreateDeviceObject, UpdateDeviceObject> {
    static final String ID = "deviceId";

    @Inject
    /**
     * <p>Constructor for Devices.</p>
     *
     * @param devices a {@link me.philcali.device.pool.service.api.DeviceRepo} object
     * @param pools a {@link me.philcali.device.pool.service.api.DevicePoolRepo} object
     */
    public Devices(final DeviceRepo devices, final DevicePoolRepo pools) {
        super(devices, pools);
    }

    /**
     * <p>locks.</p>
     *
     * @return a {@link java.lang.Class} object
     */
    @Path("/{" + ID + "}/locks")
    public Class<DeviceLocks> locks() {
        return DeviceLocks.class;
    }

    /**
     * <p>get.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @GET
    @Path("/{" + ID + "}")
    public Response get(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String deviceId) {
        return getItem(context, deviceId, poolId);
    }

    /**
     * <p>list.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param limit a int
     * @param nextToken a {@link java.lang.String} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @GET
    public Response list(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        return listItems(context, limit, nextToken, poolId);
    }

    /**
     * <p>create.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateDeviceObject} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            CreateDeviceObject create) {
        return createItem(context, create, poolId);
    }

    /**
     * <p>delete.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String deviceId) {
        return deleteItem(context, deviceId, poolId);
    }

    /**
     * <p>update.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param update a {@link me.philcali.device.pool.service.api.model.UpdateDeviceObject} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @PUT
    @Path("/{" + ID + "}")
    public Response update(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(ID) String deviceId,
            UpdateDeviceObject update) {
        UpdateDeviceObject object = UpdateDeviceObject.builder()
                .from(update)
                .id(deviceId)
                .build();
        return updateItem(context, object, poolId);
    }
}
