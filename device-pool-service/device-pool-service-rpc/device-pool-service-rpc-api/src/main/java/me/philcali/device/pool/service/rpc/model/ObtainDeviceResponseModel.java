package me.philcali.device.pool.service.rpc.model;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DeviceObject;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
abstract class ObtainDeviceResponseModel {
    abstract CompositeKey accountKey();

    abstract DeviceObject device();
}
