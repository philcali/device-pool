/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.LockRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.DeviceRepoDynamo;
import me.philcali.device.pool.service.data.LockRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.data.ReservationRepoDynamo;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Inject;

class ApplicationBinder extends AbstractBinder {
    private final ObjectMapper mapper;
    private final DevicePoolRepo poolRepo;
    private final DeviceRepo deviceRepo;
    private final ProvisionRepo provisionRepo;
    private final ReservationRepo reservationRepo;
    private final LockRepoDynamo lockRepo;

    @Inject
    ApplicationBinder(
            ObjectMapper mapper,
            DevicePoolRepoDynamo poolRepo,
            DeviceRepoDynamo deviceRepo,
            ProvisionRepoDynamo provisionRepo,
            ReservationRepoDynamo reservationRepo,
            LockRepoDynamo lockRepo) {
        this.mapper = mapper;
        this.poolRepo = poolRepo;
        this.deviceRepo = deviceRepo;
        this.provisionRepo = provisionRepo;
        this.reservationRepo = reservationRepo;
        this.lockRepo = lockRepo;
    }

    @Override
    protected void configure() {
        bind(mapper);
        bind(poolRepo).to(DevicePoolRepo.class);
        bind(deviceRepo).to(DeviceRepo.class);
        bind(provisionRepo).to(ProvisionRepo.class);
        bind(reservationRepo).to(ReservationRepo.class);
        bind(lockRepo).to(LockRepo.class);
    }
}
