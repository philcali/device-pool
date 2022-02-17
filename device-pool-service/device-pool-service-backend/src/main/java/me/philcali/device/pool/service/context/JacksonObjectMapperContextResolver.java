/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.context;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * <p>JacksonObjectMapperContextResolver class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Provider
public class JacksonObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    @Inject
    /**
     * <p>Constructor for JacksonObjectMapperContextResolver.</p>
     *
     * @param mapper a {@link com.fasterxml.jackson.databind.ObjectMapper} object
     */
    public JacksonObjectMapperContextResolver(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** {@inheritDoc} */
    @Override
    public ObjectMapper getContext(Class<?> aClass) {
        return mapper;
    }
}
