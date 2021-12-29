package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Component;
import me.philcali.device.pool.service.workflow.CreateReservationStep;
import me.philcali.device.pool.service.workflow.StartStep;

import javax.inject.Singleton;

@Component(modules = {
        JacksonModule.class,
        WorkflowModule.class,
        DynamoDBModule.class,
        ApplicationModule.class
})
@Singleton
public interface DevicePoolEventComponent {
    ObjectMapper mapper();

    StartStep startProvisioning();

    CreateReservationStep createReservationStep();
}
