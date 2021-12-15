package me.philcali.device.pool.service.dao;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnumAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TableSchemas {

    private static <R, B> StaticImmutableTableSchema.Builder<R, B> commonTable(
            Class<R> resultClass, Class<B> builderClass,
            Function<R, CompositeKey> getAccount,
            BiFunction<B, CompositeKey, B> setComposite,
            Function<R, String> getId,
            BiConsumer<B, String> setId) {
        return TableSchema.builder(resultClass, builderClass)
                .addAttribute(String.class, a -> a.name("account")
                        .getter(getAccount.andThen(CompositeKey::toString))
                        .setter((b, account) -> setComposite.apply(b, CompositeKey.fromString(account)))
                        .tags(StaticAttributeTags.primaryPartitionKey()))
                .addAttribute(String.class, a -> a.name("id")
                        .getter(getId)
                        .setter(setId)
                        .tags(StaticAttributeTags.primarySortKey()));
    }

    public static TableSchema<DevicePoolObject> poolTableSchema() {
        return commonTable(DevicePoolObject.class, DevicePoolObject.Builder.class,
                DevicePoolObject::account, DevicePoolObject.Builder::account,
                DevicePoolObject::id, DevicePoolObject.Builder::id)
                .addAttribute(String.class, a -> a.name("name")
                        .getter(DevicePoolObject::name)
                        .setter(DevicePoolObject.Builder::name))
                .build();
    }

    public static TableSchema<ProvisionObject> provisionTableSchema() {
        return commonTable(ProvisionObject.class, ProvisionObject.Builder.class,
                ProvisionObject::account, ProvisionObject.Builder::account,
                ProvisionObject::id, ProvisionObject.Builder::id)
                .addAttribute(Status.class, a -> a.name("status")
                        .getter(ProvisionObject::status)
                        .setter(ProvisionObject.Builder::status)
                        .attributeConverter(EnumAttributeConverter.create(Status.class)))
                .build();
    }

    public static TableSchema<ReservationObject> reservationTableSchema() {
        return commonTable(ReservationObject.class, ReservationObject.Builder.class,
                ReservationObject::account, ReservationObject.Builder::account,
                ReservationObject::id, ReservationObject.Builder::id)
                .addAttribute(String.class, a -> a.name("deviceId")
                        .getter(ReservationObject::deviceId)
                        .setter(ReservationObject.Builder::deviceId))
                .addAttribute(Status.class, a -> a.name("status")
                        .getter(ReservationObject::status)
                        .setter(ReservationObject.Builder::status)
                        .attributeConverter(EnumAttributeConverter.create(Status.class)))
                .build();
    }
}
