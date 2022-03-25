/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import me.philcali.device.pool.service.event.CancelProvisionWorkflowFunction;
import me.philcali.device.pool.service.event.CancelReservationFunction;
import me.philcali.device.pool.service.event.ClearResourceLockFunction;
import me.philcali.device.pool.service.event.CreateDevicePoolFunction;
import me.philcali.device.pool.service.event.DeleteDevicePoolFunction;
import me.philcali.device.pool.service.event.DeleteProvisionFunction;
import me.philcali.device.pool.service.event.DevicePoolEventRouterFunction;
import me.philcali.device.pool.service.event.StartProvisionWorkflowFunction;

import javax.inject.Singleton;

@Module
class EventRouterModule {
    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesStartProvisionFunction(StartProvisionWorkflowFunction function) {
        return function;
    }

    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesCancelProvisionFunction(CancelProvisionWorkflowFunction function) {
        return function;
    }

    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesCancelReservationFunction(CancelReservationFunction function) {
        return function;
    }

    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesDeleteDevicePoolFunction(DeleteDevicePoolFunction function) {
        return function;
    }

    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesDeleteProvisionFunction(DeleteProvisionFunction function) {
        return function;
    }

    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesClearLockFunction(ClearResourceLockFunction function) {
        return function;
    }

    @Provides
    @Singleton
    @IntoSet
    DevicePoolEventRouterFunction providesCreateDevicePoolFunction(CreateDevicePoolFunction function) {
        return function;
    }
}
