/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.module;

import dagger.Module;
import dagger.Provides;
import me.philcali.device.pool.service.workflow.Constants;
import software.amazon.awssdk.services.sfn.SfnClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
class WorkflowModule {
    @Provides
    @Singleton
    static SfnClient providesStepFunctionsClient() {
        return SfnClient.create();
    }

    @Provides
    @Singleton
    @Named(Constants.WORKFLOW_ID)
    static String providesWorkflowId() {
        return System.getenv(Constants.WORKFLOW_ID);
    }
}
