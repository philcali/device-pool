package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.DeviceRepoDynamo;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Inject;

class ApplicationBinder extends AbstractBinder {
    private final ObjectMapper mapper;
    private final DevicePoolRepo poolRepo;
    private final DeviceRepo deviceRepo;

    @Inject
    ApplicationBinder(
            ObjectMapper mapper,
            DevicePoolRepoDynamo poolRepo,
            DeviceRepoDynamo deviceRepo) {
        this.mapper = mapper;
        this.poolRepo = poolRepo;
        this.deviceRepo = deviceRepo;
    }

    @Override
    protected void configure() {
        bind(mapper);
        bind(poolRepo).to(DevicePoolRepo.class);
        bind(deviceRepo).to(DeviceRepo.class);
    }
}
