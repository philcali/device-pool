package me.philcali.device.pool.service.rpc;

import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;

public interface DevicePoolClient {
    DevicePoolEndpointType endpointType();

    ObtainDeviceResponse obtainDevice(ObtainDeviceRequest request) throws RemoteServiceException;
}
