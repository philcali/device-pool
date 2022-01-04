package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.DeviceRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.data.ReservationRepoDynamo;

import javax.inject.Singleton;

@Module
class ApplicationModule {
    @Provides
    @Singleton
    static ProvisionRepo providesProvisionRepo(ProvisionRepoDynamo dynamo) {
        return dynamo;
    }

    @Provides
    @Singleton
    static ReservationRepo providesReservationRepo(ReservationRepoDynamo dynamo) {
        return dynamo;
    }

    @Provides
    @Singleton
    static DevicePoolRepo providesDevicePoolRepo(DevicePoolRepoDynamo dynamo) {
        return dynamo;
    }

    @Provides
    @Singleton
    static DeviceRepo providesDeviceRepo(DeviceRepoDynamo dynamo) {
        return dynamo;
    }
}
