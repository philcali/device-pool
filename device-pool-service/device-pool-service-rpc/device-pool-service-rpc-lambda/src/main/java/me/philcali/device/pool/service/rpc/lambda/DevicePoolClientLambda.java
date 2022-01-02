package me.philcali.device.pool.service.rpc.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import software.amazon.awssdk.services.lambda.LambdaClient;

import javax.inject.Inject;

@AutoService(DevicePoolClient.class)
public class DevicePoolClientLambda implements DevicePoolClient {
    private final LambdaClient lambda;
    private final ObjectMapper mapper;

    @Inject
    public DevicePoolClientLambda(
            final LambdaClient lambda,
            final ObjectMapper mapper) {
        this.lambda = lambda;
        this.mapper = mapper;
    }

    public DevicePoolClientLambda() {
        this(LambdaClient.create(), new ObjectMapper());
    }

    @Override
    public DevicePoolEndpointType endpointType() {
        return DevicePoolEndpointType.LAMBDA;
    }

    @Override
    public ObtainDeviceResponse obtainDevice(ObtainDeviceRequest request) throws RemoteServiceException {
        return null;
    }
}
