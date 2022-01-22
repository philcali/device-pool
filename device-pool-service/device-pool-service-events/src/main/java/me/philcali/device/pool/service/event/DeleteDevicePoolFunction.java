package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.DeviceRepo;
import me.philcali.device.pool.service.api.ProvisionRepo;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;
import me.philcali.device.pool.service.data.DevicePoolRepoDynamo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class DeleteDevicePoolFunction implements DevicePoolEventRouterFunction {
    private static final Logger LOGGER = LogManager.getLogger(DeleteDevicePoolFunction.class);
    private final DeviceRepo deviceRepo;
    private final ProvisionRepo provisionRepo;
    private final TableSchema<DevicePoolObject> tableSchema;

    @Inject
    public DeleteDevicePoolFunction(
            final DeviceRepo deviceRepo,
            final ProvisionRepo provisionRepo,
            final TableSchema<DevicePoolObject> tableSchema) {
        this.deviceRepo = deviceRepo;
        this.provisionRepo = provisionRepo;
        this.tableSchema = tableSchema;
    }

    @Override
    public boolean test(Record record) {
        return record.getEventName().equals(OperationType.REMOVE.name())
                && primaryKeyFrom(record, StreamRecord::getOldImage).endsWith(DevicePoolRepoDynamo.RESOURCE);
    }

    @Override
    public void accept(
            Map<String, AttributeValue> newImage,
            Map<String, AttributeValue> oldImage) {
        DevicePoolObject deletedPool = tableSchema.mapToItem(oldImage);
        listAll(deletedPool.selfKey(), deviceRepo).forEach(device -> {
            LOGGER.info("Removing associated device from pool {}: {}",
                    deletedPool.id(),
                    device.id());
            deviceRepo.delete(device.key(), device.id());
        });
        listAll(deletedPool.selfKey(), provisionRepo).forEach(provisionObject -> {
            if (!provisionObject.status().isTerminal()) {
                // Send update first to cancel, then remove
                provisionRepo.update(provisionObject.key(), UpdateProvisionObject.builder()
                        .id(provisionObject.id())
                        .message("Cancellation triggered from device pool deletion.")
                        .status(Status.CANCELING)
                        .build());
            }
            provisionRepo.delete(provisionObject.key(), provisionObject.id());
            LOGGER.info("Removing associated provision from pool {}: {}",
                    deletedPool.id(),
                    provisionObject.id());
        });
    }
}
