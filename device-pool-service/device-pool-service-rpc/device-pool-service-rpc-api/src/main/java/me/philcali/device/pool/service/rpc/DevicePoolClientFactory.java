/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.rpc;

import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>DevicePoolClientFactory interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@FunctionalInterface
public interface DevicePoolClientFactory {
    /**
     * <p>get.</p>
     *
     * @param endpointType a {@link me.philcali.device.pool.service.api.model.DevicePoolEndpointType} object
     * @return a {@link me.philcali.device.pool.service.rpc.DevicePoolClient} object
     */
    DevicePoolClient get(DevicePoolEndpointType endpointType);

    /**
     * <p>createDefault.</p>
     *
     * @param loader a {@link java.lang.ClassLoader} object
     * @return a {@link me.philcali.device.pool.service.rpc.DevicePoolClientFactory} object
     */
    static DevicePoolClientFactory createDefault(final ClassLoader loader) {
        ServiceLoader<DevicePoolClient> services = ServiceLoader.load(DevicePoolClient.class, loader);
        return fromCollection(services.stream().map(ServiceLoader.Provider::get).collect(Collectors.toSet()));
    }

    /**
     * <p>fromCollection.</p>
     *
     * @param clients a {@link java.util.Collection} object
     * @return a {@link me.philcali.device.pool.service.rpc.DevicePoolClientFactory} object
     */
    static DevicePoolClientFactory fromCollection(Collection<DevicePoolClient> clients) {
        final Map<DevicePoolEndpointType, DevicePoolClient> typeToClient = clients.stream()
                .collect(Collectors.toMap(
                        DevicePoolClient::endpointType,
                        Function.identity()
                ));
        return typeToClient::get;
    }
}
