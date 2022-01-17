package me.philcali.device.pool.service.rpc;

import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.CancelReservationResponse;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;

public interface DevicePoolClient {
    DevicePoolEndpointType endpointType();

    ObtainDeviceResponse obtainDevice(Context context, ObtainDeviceRequest request) throws RemoteServiceException;

    CancelReservationResponse cancelReservation(Context context, CancelReservationRequest request)
        throws RemoteServiceException;
}
