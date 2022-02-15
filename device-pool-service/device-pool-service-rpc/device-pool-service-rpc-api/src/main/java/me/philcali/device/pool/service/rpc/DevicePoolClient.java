/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.rpc;

import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.CancelReservationResponse;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;

/**
 * Abstraction for obtaining {@link me.philcali.device.pool.Device} and canceling held
 * {@link me.philcali.device.pool.model.Reservation} delegated from the provision workflow.
 */
public interface DevicePoolClient {

    /**
     * The type of remote endpoint this {@link DevicePoolClient} can handle.
     *
     * @return The {@link DevicePoolEndpointType} of this client
     */
    DevicePoolEndpointType endpointType();

    /**
     * The entry point for obtaining devices from the provision workflow. Calls to obtain
     * devices are idempotent.
     *
     * @param context The {@link Context} surrounding obtaining devices
     * @param request The underlying the {@link ObtainDeviceRequest}
     * @return The result of obtaining device call in the form of an {@link ObtainDeviceResponse}
     * @throws RemoteServiceException Failure to obtain devices from a remote source
     */
    ObtainDeviceResponse obtainDevice(Context context, ObtainDeviceRequest request)
            throws RemoteServiceException;

    /**
     * The entry point for canceling a held reservation to some remote authority. This method
     * is invoked as part of the provisioning workflow, but directly from an explicit or implicit
     * provision cancellation request.
     *
     * @param context The {@link Context} of the cancellation request
     * @param request The cancellation request in the {@link CancelReservationRequest} model
     * @return The result of the method in the form of a {@link CancelReservationResponse}
     * @throws RemoteServiceException Failure to cancel held reservation from a remote source
     */
    CancelReservationResponse cancelReservation(Context context, CancelReservationRequest request)
        throws RemoteServiceException;
}
