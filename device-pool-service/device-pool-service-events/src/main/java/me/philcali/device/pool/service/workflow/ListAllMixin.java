/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.service.api.ObjectRepository;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * <p>ListAllMixin interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface ListAllMixin {
    /**
     * <p>listAll.</p>
     *
     * @param key a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param repository a {@link me.philcali.device.pool.service.api.ObjectRepository} object
     * @param <T> a T class
     * @return a {@link java.util.List} object
     */
    default <T> List<T> listAll(CompositeKey key, ObjectRepository<T, ?, ?> repository) {
        List<T> existingObjects = new ArrayList<>();
        QueryResults<T> results = null;
        do {
            results = repository.list(key, QueryParams.builder()
                    .limit(ObjectRepository.MAX_ITEMS)
                    .nextToken(Optional.ofNullable(results).map(QueryResults::nextToken).orElse(null))
                    .build());
            existingObjects.addAll(results.results());
        } while (results.isTruncated());
        return Collections.unmodifiableList(existingObjects);
    }
}
