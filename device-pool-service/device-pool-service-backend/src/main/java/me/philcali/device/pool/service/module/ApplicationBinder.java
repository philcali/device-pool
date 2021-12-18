package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Inject;

class ApplicationBinder extends AbstractBinder {
    private final ObjectMapper mapper;
    private final DevicePoolRepo poolRepo;

    @Inject
    ApplicationBinder(
            ObjectMapper mapper,
            DevicePoolRepoDynamo poolRepo) {
        this.mapper = mapper;
        this.poolRepo = poolRepo;
    }

    @Override
    protected void configure() {
        bind(mapper);
        bind(poolRepo).to(DevicePoolRepo.class);
    }
}
