/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.api.DevicePoolRepo;
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

@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class PoolLocks extends RepositoryResource<LockObject, CreateLockObject, UpdateLockObject> {
    @Inject
    public PoolLocks(final LockRepo locks, final DevicePoolRepo poolRepo) {
        super(locks, poolRepo);
    }

    @GET
    public Response get(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId) {
        return getItem(context, LockRepo.SINGLETON, poolId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            CreateLockObject create) {
        return createItem(context, create, poolId);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response extend(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId,
            UpdateLockObject update) {
        return updateItem(context, update, poolId);
    }

    @DELETE
    public Response delete(
            @Context SecurityContext context,
            @PathParam(Pools.ID) String poolId) {
        return deleteItem(context, LockRepo.SINGLETON, poolId);
    }
}
