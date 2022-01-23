/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.resource;

import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.ObjectRepository;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.UniqueEntity;
import software.amazon.awssdk.arns.Arn;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;
import java.util.List;

abstract class RepositoryResource<T, C, U> {
    protected final ObjectRepository<T, C, U> repository;
    protected final List<ObjectRepository<? extends UniqueEntity, ?, ?>> parents;

    RepositoryResource(
            final ObjectRepository<T, C, U> repository,
            final ObjectRepository<? extends UniqueEntity, ?, ?> ... parents) {
        this.repository = repository;
        this.parents = Arrays.asList(parents);
    }

    protected CompositeKey toKey(SecurityContext context, String...parentIds) {
        String username = context.getUserPrincipal().getName();
        if (username.startsWith("arn:")) {
            username = Arn.fromString(username).accountId().orElse(username);
        }
        CompositeKey account = CompositeKey.of(username);
        for (int index = 0; index < parentIds.length; index++) {
            account = CompositeKey.builder()
                    .from(parents.get(index).get(account, parentIds[index]).key())
                    .addResources(parentIds[index])
                    .build();
        }
        return account;
    }

    protected Response listItems(SecurityContext context, int limit, String nextToken, String...parentIds) {
        if (limit <= 0 || limit > DevicePoolRepo.MAX_ITEMS) {
            limit = DevicePoolRepo.MAX_ITEMS;
        }
        QueryResults<T> objects = repository.list(toKey(context, parentIds),
                QueryParams.builder()
                        .limit(limit)
                        .nextToken(nextToken)
                        .build());
        return Response.ok(objects).build();
    }

    protected Response getItem(SecurityContext context, String selfId, String...parentIds) {
        return Response.ok(repository.get(toKey(context, parentIds), selfId)).build();
    }

    protected Response deleteItem(SecurityContext context, String selfId, String...parentIds) {
        repository.delete(toKey(context, parentIds), selfId);
        return Response.noContent().build();
    }

    protected Response createItem(SecurityContext context, C create, String...parentIds) {
        return Response.ok(repository.create(toKey(context, parentIds), create)).build();
    }

    protected Response updateItem(SecurityContext context, U update, String...parentIds) {
        return Response.accepted(repository.update(toKey(context, parentIds), update)).build();
    }
}
