/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

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

/**
 * <p>Pools class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Singleton
@Path("/pools")
@Produces(MediaType.APPLICATION_JSON)
public class Pools extends RepositoryResource<DevicePoolObject, CreateDevicePoolObject, UpdateDevicePoolObject> {
    static final String ID = "poolId";

    @Inject
    /**
     * <p>Constructor for Pools.</p>
     *
     * @param poolRepo a {@link me.philcali.device.pool.service.api.DevicePoolRepo} object
     */
    public Pools(DevicePoolRepo poolRepo) {
        super(poolRepo);
    }

    /**
     * <p>get.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @GET
    @Path("/{" + ID + "}")
    public Response get(@Context SecurityContext context, @PathParam(ID) String poolId) {
        return getItem(context, poolId);
    }

    /**
     * <p>devices.</p>
     *
     * @return a {@link java.lang.Class} object
     */
    @Path("/{" + ID + "}/devices")
    public Class<Devices> devices() {
        return Devices.class;
    }

    /**
     * <p>provisions.</p>
     *
     * @return a {@link java.lang.Class} object
     */
    @Path("/{" + ID + "}/provisions")
    public Class<Provisions> provisions() {
        return Provisions.class;
    }

    /**
     * <p>locks.</p>
     *
     * @return a {@link java.lang.Class} object
     */
    @Path("/{" + ID + "}/locks")
    public Class<PoolLocks> locks() {
        return PoolLocks.class;
    }

    /**
     * <p>list.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param limit a int
     * @param nextToken a {@link java.lang.String} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @GET
    public Response list(
            @Context SecurityContext context,
            @QueryParam("limit") int limit,
            @QueryParam("nextToken") String nextToken) {
        return listItems(context, limit, nextToken);
    }

    /**
     * <p>create.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param input a {@link me.philcali.device.pool.service.api.model.CreateDevicePoolObject} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext context, CreateDevicePoolObject input) {
        return createItem(context, input);
    }

    /**
     * <p>update.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param input a {@link me.philcali.device.pool.service.api.model.UpdateDevicePoolObject} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(
            @Context SecurityContext context,
            @PathParam(ID) String poolId,
            UpdateDevicePoolObject input) {
        return updateItem(context, UpdateDevicePoolObject.builder().from(input).name(poolId).build());
    }

    /**
     * <p>delete.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @DELETE
    @Path("/{" + ID + "}")
    public Response delete(@Context SecurityContext context, @PathParam(ID) String poolId) {
        return deleteItem(context, poolId);
    }
}
