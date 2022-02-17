/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Component;
import me.philcali.device.pool.service.workflow.CreateReservationStep;
import me.philcali.device.pool.service.event.DevicePoolEventRouter;
import me.philcali.device.pool.service.workflow.FailProvisionStep;
import me.philcali.device.pool.service.workflow.FinishProvisionStep;
import me.philcali.device.pool.service.workflow.ObtainDevicesStep;
import me.philcali.device.pool.service.workflow.StartProvisionStep;

import javax.inject.Singleton;

/**
 * <p>DevicePoolEventComponent interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@Component(modules = {
        JacksonModule.class,
        WorkflowModule.class,
        DynamoDBModule.class,
        ApplicationModule.class,
        HttpModule.class,
        LambdaModule.class,
        DevicePoolClientModule.class,
        EventRouterModule.class
})
@Singleton
public interface DevicePoolEventComponent {
    /**
     * <p>mapper.</p>
     *
     * @return a {@link com.fasterxml.jackson.databind.ObjectMapper} object
     */
    ObjectMapper mapper();

    /**
     * <p>eventRouter.</p>
     *
     * @return a {@link me.philcali.device.pool.service.event.DevicePoolEventRouter} object
     */
    DevicePoolEventRouter eventRouter();

    /**
     * <p>startProvisionStep.</p>
     *
     * @return a {@link me.philcali.device.pool.service.workflow.StartProvisionStep} object
     */
    StartProvisionStep startProvisionStep();

    /**
     * <p>createReservationStep.</p>
     *
     * @return a {@link me.philcali.device.pool.service.workflow.CreateReservationStep} object
     */
    CreateReservationStep createReservationStep();

    /**
     * <p>failProvisionStep.</p>
     *
     * @return a {@link me.philcali.device.pool.service.workflow.FailProvisionStep} object
     */
    FailProvisionStep failProvisionStep();

    /**
     * <p>finishProvisionStep.</p>
     *
     * @return a {@link me.philcali.device.pool.service.workflow.FinishProvisionStep} object
     */
    FinishProvisionStep finishProvisionStep();

    /**
     * <p>obtainDevicesStep.</p>
     *
     * @return a {@link me.philcali.device.pool.service.workflow.ObtainDevicesStep} object
     */
    ObtainDevicesStep obtainDevicesStep();
}
