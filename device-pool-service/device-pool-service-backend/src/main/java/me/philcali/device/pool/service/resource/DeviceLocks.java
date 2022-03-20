/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.model.CreateLockObject;
import me.philcali.device.pool.service.api.model.LockObject;
import me.philcali.device.pool.service.api.model.UpdateLockObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * <p>DeviceLocks class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class DeviceLocks extends RepositoryResource<LockObject, CreateLockObject, UpdateLockObject> {
    /**
     * <p>Constructor for DeviceLocks.</p>
     *
     * @param lockRepo a {@link me.philcali.device.pool.service.api.LockRepo} object
     * @param poolRepo a {@link me.philcali.device.pool.service.api.DevicePoolRepo} object
     * @param deviceRepo a {@link me.philcali.device.pool.service.api.DeviceRepo} object
     */
    @Inject
    public DeviceLocks(
            final LockRepo lockRepo,
            final DevicePoolRepo poolRepo,
            final DeviceRepo deviceRepo) {
        super(lockRepo, poolRepo, deviceRepo);
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
    public Response get(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Devices.ID) String deviceId) {
        return getItem(context, LockRepo.SINGLETON, poolId, deviceId);
    }

    /**
     * <p>create.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param create a {@link me.philcali.device.pool.service.api.model.CreateLockObject} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Devices.ID) String deviceId,
            CreateLockObject create) {
        return createItem(context, create, poolId, deviceId);
    }

    /**
     * <p>extend.</p>
     *
     * @param context a {@link javax.ws.rs.core.SecurityContext} object
     * @param poolId a {@link java.lang.String} object
     * @param deviceId a {@link java.lang.String} object
     * @param update a {@link me.philcali.device.pool.service.api.model.UpdateLockObject} object
     * @return a {@link javax.ws.rs.core.Response} object
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response extend(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Devices.ID) String deviceId,
            UpdateLockObject update) {
        return updateItem(context, update, poolId, deviceId);
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
    public Response delete(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            @PathParam(Devices.ID) String deviceId) {
        return deleteItem(context, LockRepo.SINGLETON, poolId, deviceId);
    }
}
