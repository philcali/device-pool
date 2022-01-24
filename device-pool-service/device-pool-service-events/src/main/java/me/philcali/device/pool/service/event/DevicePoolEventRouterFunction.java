/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import me.philcali.device.pool.service.workflow.ListAllMixin;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;


public interface DevicePoolEventRouterFunction
        extends ListAllMixin, Predicate<Record>, BiConsumer<Map<String, AttributeValue>, Map<String, AttributeValue>> {
    String PK = "PK";

    default String primaryKey(Record record) {
        return primaryKeyFrom(record, StreamRecord::getNewImage);
    }

    default String primaryKeyFrom(
            Record record,
            Function<StreamRecord, Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue>> getImage) {
        return getImage.apply(record.getDynamodb()).get(PK).getS();
    }

    @Override
    boolean test(Record record);

    @Override
    void accept(Map<String, AttributeValue> newImage, Map<String, AttributeValue> oldImage);
}
