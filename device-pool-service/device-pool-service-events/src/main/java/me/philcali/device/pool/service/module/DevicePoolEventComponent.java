package me.philcali.device.pool.service.module;

import dagger.Component;
import me.philcali.device.pool.service.workflow.StartStep;

import javax.inject.Singleton;

@Component(modules = {
        JacksonModule.class,
        WorkflowModule.class
})
@Singleton
public interface DevicePoolEventComponent {
    StartStep startProvisioning();
}
