package me.philcali.device.pool.service.rpc.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.Context;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceRequest;
import me.philcali.device.pool.service.rpc.model.ObtainDeviceResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import software.amazon.awssdk.services.lambda.model.LogType;

import javax.inject.Inject;
import java.io.IOException;

public class DevicePoolClientLambda implements DevicePoolClient {
    private static final Logger LOGGER = LogManager.getLogger(DevicePoolClientLambda.class);
    private final LambdaClient lambda;
    private final ObjectMapper mapper;

    @Inject
    public DevicePoolClientLambda(
            final LambdaClient lambda,
            final ObjectMapper mapper) {
        this.lambda = lambda;
        this.mapper = mapper;
    }

    @Override
    public DevicePoolEndpointType endpointType() {
        return DevicePoolEndpointType.LAMBDA;
    }

    @Override
    public ObtainDeviceResponse obtainDevice(Context context, ObtainDeviceRequest request)
            throws RemoteServiceException {
        try {
            InvokeResponse response = lambda.invoke(InvokeRequest.builder()
                    .functionName(context.endpoint().uri())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .logType(LogType.TAIL)
                    .payload(SdkBytes.fromUtf8String(mapper.writeValueAsString(request)))
                    .build());
            LOGGER.info("Invoked {}: {}", context.endpoint().uri(), response.logResult());
            if (response.statusCode() > 200) {
                throw new RemoteServiceException(response.functionError());
            }
            return mapper.readValue(response.payload().asByteArray(), ObtainDeviceResponse.class);
        } catch (IOException e) {
            throw new RemoteServiceException(e);
        } catch (LambdaException e) {
            LOGGER.error("Failed to invoke {}", context.endpoint().uri(), e);
            throw new RemoteServiceException(e);
        }
    }
}
