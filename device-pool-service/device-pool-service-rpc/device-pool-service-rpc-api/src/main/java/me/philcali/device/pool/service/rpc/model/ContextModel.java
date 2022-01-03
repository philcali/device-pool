package me.philcali.device.pool.service.rpc.model;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
abstract class ContextModel {
    abstract DevicePoolEndpoint endpoint();
}
