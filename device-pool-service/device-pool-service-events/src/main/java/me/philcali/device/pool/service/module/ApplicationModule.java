/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import me.philcali.device.pool.service.api.DeviceLockRepo;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.data.DeviceLockRepoDynamo;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import me.philcali.device.pool.service.data.DeviceRepoDynamo;
import me.philcali.device.pool.service.data.ProvisionRepoDynamo;
import me.philcali.device.pool.service.data.ReservationRepoDynamo;
import me.philcali.device.pool.service.data.TableSchemas;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

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

    @Provides
    @Singleton
    static TableSchema<ProvisionObject> providesProvisionSchema() {
        return TableSchemas.provisionTableSchema();
    }

    @Provides
    @Singleton
    static TableSchema<ReservationObject> providesReservationSchema() {
        return TableSchemas.reservationTableSchema();
    }

    @Provides
    @Singleton
    static TableSchema<DevicePoolObject> providesDevicePoolSchema() {
        return TableSchemas.poolTableSchema();
    }

    @Provides
    @Singleton
    static DeviceLockRepo providesDeviceLockRepo(DeviceLockRepoDynamo dynamo) {
        return dynamo;
    }
}
