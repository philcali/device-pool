/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.event;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class DevicePoolEventRouter implements Consumer<DynamodbEvent> {
    private static final Logger LOGGER = LogManager.getLogger(DevicePoolEventRouter.class);
    private final Set<DevicePoolEventRouterFunction> functions;

    @Inject
    public DevicePoolEventRouter(final Set<DevicePoolEventRouterFunction> functions) {
        this.functions = functions;
    }

    private AttributeValue convertAttribute(
            com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value) {
        AttributeValue.Builder builder = AttributeValue.builder();
        builder.s(value.getS());
        builder.n(value.getN());
        builder.bool(value.getBOOL());
        if (value.getB() != null ) {
            builder.b(SdkBytes.fromByteBuffer(value.getB()));
        }
        if (value.getL() != null) {
            builder.l(value.getL().stream()
                    .map(this::convertAttribute)
                    .collect(Collectors.toList()));
        }
        if (value.getM() != null) {
            builder.m(value.getM().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            v -> convertAttribute(v.getValue()))));
        }
        return builder.build();
    }

    private Map<String, AttributeValue> convertAttributes(
            Record record,
            Function<StreamRecord,
                    Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue>> thunk) {
        Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> image = thunk.apply(record.getDynamodb());
        if (image == null) {
            return null;
        }
        return image.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        value -> convertAttribute(value.getValue())));
    }

    @Override
    public void accept(DynamodbEvent dynamodbEvent) {
        dynamodbEvent.getRecords().forEach(record -> {
            LOGGER.debug("Found record {}", record);
            functions.stream()
                    .filter(function -> function.test(record))
                    .peek(function -> LOGGER.debug("Function {} satisfies record {}",
                            function.getClass().getSimpleName(),
                            record))
                    .findFirst()
                    .ifPresent(function -> function.accept(
                            convertAttributes(record, StreamRecord::getNewImage),
                            convertAttributes(record, StreamRecord::getOldImage)));
        });
    }
}
