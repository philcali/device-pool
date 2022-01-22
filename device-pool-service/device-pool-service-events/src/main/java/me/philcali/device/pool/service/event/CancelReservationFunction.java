package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DevicePoolRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.ReservationRepo;
import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.exception.ServiceException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;
import me.philcali.device.pool.service.data.ReservationRepoDynamo;
import me.philcali.device.pool.service.exception.RetryableException;
import me.philcali.device.pool.service.rpc.DevicePoolClient;
import me.philcali.device.pool.service.rpc.DevicePoolClientFactory;
import me.philcali.device.pool.service.rpc.exception.RemoteServiceException;
import me.philcali.device.pool.service.rpc.model.CancelReservationRequest;
import me.philcali.device.pool.service.rpc.model.CancelReservationResponse;
import me.philcali.device.pool.service.rpc.model.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class CancelReservationFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LogManager.getLogger(CancelReservationFunction.class);
    private final ReservationRepo reservationRepo;
    private final ProvisionRepo provisionRepo;
    private final DevicePoolRepo devicePoolRepo;
    private final TableSchema<ReservationObject> reservationSchema;
    private final DevicePoolClientFactory clientFactory;

    @Inject
    public CancelReservationFunction(
            DevicePoolRepo devicePoolRepo,
            ProvisionRepo provisionRepo,
            ReservationRepo reservationRepo,
            DevicePoolClientFactory clientFactory,
            TableSchema<ReservationObject> reservationSchema) {
        this.provisionRepo = provisionRepo;
        this.devicePoolRepo = devicePoolRepo;
        this.reservationRepo = reservationRepo;
        this.clientFactory = clientFactory;
        this.reservationSchema = reservationSchema;
    }

    @Override
    public boolean test(Record record) {
        return record.getEventName().equals(OperationType.MODIFY.name())
                && primaryKey(record).endsWith(ReservationRepoDynamo.RESOURCE)
                && record.getDynamodb().getNewImage().get("status").equals(Status.CANCELING.name());
    }

    @Override
    public void accept(
            Map<String, AttributeValue> newImage,
            Map<String, AttributeValue> oldImage) {
        ReservationObject reservation = reservationSchema.mapToItem(newImage);
        try {
            DevicePoolObject devicePool = devicePoolRepo.get(
                    CompositeKey.of(reservation.key().account()),
                    reservation.poolId());
            Status statusToUpdate = Status.CANCELED;
            String message = null;
            if (devicePool.type().equals(DevicePoolType.UNMANAGED)) {
                LOGGER.debug("Communicate to pool endpoint {}", devicePool.endpoint());
                ProvisionObject provision = provisionRepo.get(
                        reservation.provisionKey(),
                        reservation.provisionId());
                DevicePoolClient client = clientFactory.get(devicePool.endpoint().type());
                Context ctx = Context.of(devicePool.endpoint());
                CancelReservationResponse response = client.cancelReservation(ctx, CancelReservationRequest.builder()
                        .accountKey(provision.key())
                        .provision(provision)
                        .reservation(reservation)
                        .build());
                LOGGER.info("Response from remote {}: {}", devicePool.endpoint().uri(), response);
                if (!response.status().isTerminal()) {
                    throw new RetryableException("Endpoint " + devicePool.endpoint().uri() + " did not terminate.");
                }
                statusToUpdate = response.status();
                message = response.message();
            }
            reservationRepo.update(reservation.key().parentKey(), UpdateReservationObject.builder()
                    .id(reservation.id())
                    .status(statusToUpdate)
                    .message(message)
                    .build());
        } catch (NotFoundException e) {
            LOGGER.error("Failed to find parent resources, skipping", e);
        } catch (RemoteServiceException e) {
            if (e.isRetryable()) {
                throw e;
            }
            LOGGER.error("Failed to communicate to remote: ", e);
        } catch (ServiceException e) {
            LOGGER.error("Failed to cancel reservation {}", reservation, e);
        }
    }
}
