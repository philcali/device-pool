/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.unmanaged.operation;

import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ObtainDeviceFunction implements OperationFunction<ObtainDeviceRequest, ObtainDeviceResponse> {
    private final SsmClient ssm;

    @Inject
    public ObtainDeviceFunction(final SsmClient ssm) {
        this.ssm = ssm;
    }

    @Override
    public Class<ObtainDeviceRequest> inputType() {
        return ObtainDeviceRequest.class;
    }

    @Override
    public ObtainDeviceResponse apply(ObtainDeviceRequest obtainDeviceRequest) {
        return null;
    }
}
