package me.philcali.device.pool.service.workflow;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;


public interface DevicePoolEventRouterFunction
        extends ListAllMixin, Predicate<Record>, BiConsumer<Map<String, AttributeValue>, Map<String, AttributeValue>> {
    String PK = "PK";

    default String primaryKey(Record record) {
        return record.getDynamodb().getNewImage().get(PK).getS();
    }

    @Override
    boolean test(Record record);

    @Override
    void accept(Map<String, AttributeValue> newImage, Map<String, AttributeValue> oldImage);
}
