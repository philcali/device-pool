package me.philcali.device.pool.service.rpc.model;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ApiModel
@Value.Immutable
abstract class ObtainDeviceRequestModel {
    abstract CompositeKey accountKey();

    abstract ProvisionObject provision();

    @Nullable
    abstract ReservationObject reservation();
}
