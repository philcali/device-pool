package me.philcali.device.pool.service.rpc.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@AutoService(DevicePoolClient.class)
public class DevicePoolClientHttp implements DevicePoolClient {
    private static final Logger LOGGER = LogManager.getLogger(DevicePoolClientHttp.class);
    private final HttpClient client;
    private final ObjectMapper mapper;

    @Inject
    public DevicePoolClientHttp(
            final HttpClient client,
            final ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public DevicePoolClientHttp() {
        this(HttpClient.newHttpClient(), new ObjectMapper());
    }

    @Override
    public DevicePoolEndpointType endpointType() {
        return DevicePoolEndpointType.HTTP;
    }

    @Override
    public ObtainDeviceResponse obtainDevice(Context context, ObtainDeviceRequest request)
            throws RemoteServiceException {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(context.endpoint().uri()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", request.accountKey().account())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                LOGGER.error("Invoking {} resulted in {}", context.endpoint().uri(), response.statusCode());
                throw new RemoteServiceException(new String(response.body(), StandardCharsets.UTF_8));
            }
            return mapper.readValue(response.body(), ObtainDeviceResponse.class);
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to parse {}", context.endpoint().uri(), e);
            throw new RemoteServiceException(e);
        } catch (IOException e) {
            LOGGER.error("Failed to invoke {}", context.endpoint().uri(), e);
            throw new RemoteServiceException(e);
        } catch (InterruptedException e) {
            throw new RemoteServiceException(e);
        }
    }
}
