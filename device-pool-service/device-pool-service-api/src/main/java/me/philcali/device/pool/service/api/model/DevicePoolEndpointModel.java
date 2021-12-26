package me.philcali.device.pool.service.api.model;

import me.philcali.device.pool.model.ApiModel;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
interface DevicePoolEndpointModel {
    String uri();

    DevicePoolEndpointType type();
}
