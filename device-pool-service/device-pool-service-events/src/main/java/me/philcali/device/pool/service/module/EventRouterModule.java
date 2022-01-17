package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import me.philcali.device.pool.service.workflow.CancelProvisionWorkflowFunction;
import me.philcali.device.pool.service.workflow.CancelReservationFunction;
import me.philcali.device.pool.service.workflow.DevicePoolEventRouterFunction;
import me.philcali.device.pool.service.workflow.StartProvisionWorkflowFunction;

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
}
