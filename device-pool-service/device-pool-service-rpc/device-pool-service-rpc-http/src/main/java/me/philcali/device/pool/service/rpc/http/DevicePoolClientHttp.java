package me.philcali.device.pool.service.rpc.http;

import com.google.auto.service.AutoService;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;

import javax.inject.Inject;
import java.net.http.HttpClient;

@AutoService(DevicePoolClient.class)
public class DevicePoolClientHttp implements DevicePoolClient {
    private final HttpClient client;

    @Inject
    public DevicePoolClientHttp(final HttpClient client) {
        this.client = client;
    }

    public DevicePoolClientHttp() {
        this(HttpClient.newHttpClient());
    }

    @Override
    public DevicePoolEndpointType endpointType() {
        return DevicePoolEndpointType.HTTP;
    }

    @Override
    public ObtainDeviceResponse obtainDevice(ObtainDeviceRequest request) throws RemoteServiceException {
        return null;
    }
}
