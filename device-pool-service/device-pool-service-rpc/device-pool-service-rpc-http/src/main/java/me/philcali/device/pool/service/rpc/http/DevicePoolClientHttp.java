/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.rpc.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.CancelReservationResponse;
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
import java.util.Set;

/**
 * <p>DevicePoolClientHttp class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class DevicePoolClientHttp implements DevicePoolClient {
    private static final Logger LOGGER = LogManager.getLogger(DevicePoolClientHttp.class);
    private final HttpClient client;
    private final ObjectMapper mapper;
    private static final Set<Integer> RETRY_CODES = Set.of(
            408, 429,
            500, 502, 503, 504);

    @Inject
    /**
     * <p>Constructor for DevicePoolClientHttp.</p>
     *
     * @param client a HttpClient object
     * @param mapper a {@link com.fasterxml.jackson.databind.ObjectMapper} object
     */
    public DevicePoolClientHttp(
            final HttpClient client,
            final ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** {@inheritDoc} */
    @Override
    public DevicePoolEndpointType endpointType() {
        return DevicePoolEndpointType.HTTP;
    }

    private <Req, Res> Res invoke(
            Context context,
            CompositeKey accountKey,
            Req req,
            Class<Res> responseClass) throws RemoteServiceException {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(context.endpoint().uri()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", accountKey.account())
                    .header("X-Operation", req.getClass().getSimpleName().replace("Request", ""))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(req), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                LOGGER.error("Invoking {} resulted in {}", context.endpoint().uri(), response.statusCode());
                throw new RemoteServiceException(
                        new String(response.body(), StandardCharsets.UTF_8),
                        RETRY_CODES.contains(response.statusCode()));
            }
            return mapper.readValue(response.body(), responseClass);
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to parse {}", context.endpoint().uri(), e);
            throw new RemoteServiceException(e);
        } catch (IOException e) {
            LOGGER.error("Failed to invoke {}", context.endpoint().uri(), e);
            throw new RemoteServiceException(e);
        } catch (InterruptedException e) {
            throw new RemoteServiceException(e, true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CancelReservationResponse cancelReservation(Context context, CancelReservationRequest request)
            throws RemoteServiceException {
        return invoke(context, request.accountKey(), request, CancelReservationResponse.class);
    }

    /** {@inheritDoc} */
    @Override
    public ObtainDeviceResponse obtainDevice(Context context, ObtainDeviceRequest request)
            throws RemoteServiceException {
        return invoke(context, request.accountKey(), request, ObtainDeviceResponse.class);
    }
}
