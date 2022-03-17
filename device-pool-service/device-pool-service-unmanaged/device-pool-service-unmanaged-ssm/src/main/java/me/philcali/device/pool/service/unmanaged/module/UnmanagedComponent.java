/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.module;

import dagger.Component;
import me.philcali.device.pool.service.module.DynamoDBModule;
import me.philcali.device.pool.service.module.JacksonModule;
import me.philcali.device.pool.service.unmanaged.OperationHandler;

import javax.inject.Singleton;

@Component(modules = {
        DynamoDBModule.class,
        JacksonModule.class,
        SSMModule.class,
        OperationModule.class
})
@Singleton
public interface UnmanagedComponent {
    OperationHandler handler();
}
