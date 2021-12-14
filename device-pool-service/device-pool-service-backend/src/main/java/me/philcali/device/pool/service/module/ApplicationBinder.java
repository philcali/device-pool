package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Inject;

class ApplicationBinder extends AbstractBinder {
    private final ObjectMapper mapper;

    @Inject
    ApplicationBinder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void configure() {
        bind(mapper);
    }
}
