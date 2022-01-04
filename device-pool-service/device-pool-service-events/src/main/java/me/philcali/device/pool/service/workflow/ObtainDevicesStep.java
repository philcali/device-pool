package me.philcali.device.pool.service.workflow;

import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.exception.WorkflowExecutionException;
import me.philcali.device.pool.service.model.WorkflowState;
import me.philcali.device.pool.service.rpc.DevicePoolClientFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ObtainDevicesStep implements WorkflowStep<WorkflowState, WorkflowState> {
    private final ProvisionRepo provisionRepo;
    private final DevicePoolClientFactory clientFactory;

    @Inject
    public ObtainDevicesStep(
            final ProvisionRepo provisionRepo,
            final DevicePoolClientFactory clientFactory) {
        this.provisionRepo = provisionRepo;
        this.clientFactory = clientFactory;
    }

    @Override
    public WorkflowState execute(WorkflowState input) throws WorkflowExecutionException, RetryableException {
        return null;
    }
}
