package me.philcali.device.pool.service.data;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnumAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;

import java.time.Instant;
import java.util.Optional;
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
                .addAttribute(String.class, a -> a.name("PK")
                        .getter(getAccount.andThen(CompositeKey::toString))
                        .setter((b, account) -> setComposite.apply(b, CompositeKey.fromString(account)))
                        .tags(StaticAttributeTags.primaryPartitionKey()))
                .addAttribute(String.class, a -> a.name("SK")
                        .getter(getId)
                        .setter(setId)
                        .tags(StaticAttributeTags.primarySortKey()));
    }

    public static TableSchema<DevicePoolObject> poolTableSchema() {
        return commonTable(DevicePoolObject.class, DevicePoolObject.Builder.class,
                DevicePoolObject::key, DevicePoolObject.Builder::key,
                DevicePoolObject::name, DevicePoolObject.Builder::name)
                .newItemBuilder(DevicePoolObject::builder, DevicePoolObject.Builder::build)
                .addAttribute(String.class, a -> a.name("description")
                        .getter(DevicePoolObject::description)
                        .setter(DevicePoolObject.Builder::description))
                .addAttribute(Long.class, a -> a.name("createdAt")
                        .getter(pool -> Optional.ofNullable(pool.createdAt()).map(Instant::getEpochSecond).orElse(null))
                        .setter((builder, value) -> builder.createdAt(Instant.ofEpochSecond(value))))
                .addAttribute(Long.class, a -> a.name("updatedAt")
                        .getter(pool -> pool.updatedAt().getEpochSecond())
                        .setter((builder, value) -> builder.updatedAt(Instant.ofEpochSecond(value))))
                .build();
    }

    public static TableSchema<ProvisionObject> provisionTableSchema() {
        return commonTable(ProvisionObject.class, ProvisionObject.Builder.class,
                ProvisionObject::key, ProvisionObject.Builder::key,
                ProvisionObject::id, ProvisionObject.Builder::id)
                .newItemBuilder(ProvisionObject::builder, ProvisionObject.Builder::build)
                .addAttribute(Status.class, a -> a.name("status")
                        .getter(ProvisionObject::status)
                        .setter(ProvisionObject.Builder::status)
                        .attributeConverter(EnumAttributeConverter.create(Status.class)))
                .addAttribute(String.class, a -> a.name("message")
                        .getter(ProvisionObject::message)
                        .setter(ProvisionObject.Builder::message))
                .addAttribute(Long.class, a -> a.name("createdAt")
                        .getter(p -> Optional.ofNullable(p.createdAt()).map(Instant::getEpochSecond).orElse(null))
                        .setter((builder, value) -> builder.createdAt(Instant.ofEpochSecond(value))))
                .addAttribute(Long.class, a -> a.name("updatedAt")
                        .getter(p -> p.updatedAt().getEpochSecond())
                        .setter((builder, value) -> builder.updatedAt(Instant.ofEpochSecond(value))))
                .addAttribute(Long.class, a -> a.name("expiresIn")
                        .getter(p -> Optional.ofNullable(p.expiresIn()).map(Instant::getEpochSecond).orElse(null))
                        .setter((builder, value) -> builder.expiresIn(Instant.ofEpochSecond(value))))
                .build();
    }

    public static TableSchema<ReservationObject> reservationTableSchema() {
        return commonTable(ReservationObject.class, ReservationObject.Builder.class,
                ReservationObject::key, ReservationObject.Builder::key,
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

    public static TableSchema<DeviceObject> deviceSchema() {
        return commonTable(DeviceObject.class, DeviceObject.Builder.class,
                DeviceObject::key, DeviceObject.Builder::key,
                DeviceObject::id, DeviceObject.Builder::id)
                .newItemBuilder(DeviceObject::builder, DeviceObject.Builder::build)
                .addAttribute(String.class, a -> a.name("publicAddress")
                        .getter(DeviceObject::publicAddress)
                        .setter(DeviceObject.Builder::publicAddress))
                .addAttribute(String.class, a -> a.name("privateAddress")
                        .getter(DeviceObject::privateAddress)
                        .setter(DeviceObject.Builder::privateAddress))
                .addAttribute(Long.class, a -> a.name("createdAt")
                        .getter(d -> Optional.ofNullable(d.createdAt()).map(Instant::getEpochSecond).orElse(null))
                        .setter((builder, value) -> builder.createdAt(Instant.ofEpochSecond(value))))
                .addAttribute(Long.class, a -> a.name("updatedAt")
                        .getter(pool -> pool.updatedAt().getEpochSecond())
                        .setter((builder, value) -> builder.updatedAt(Instant.ofEpochSecond(value))))
                .addAttribute(Long.class, a -> a.name("expiresIn")
                        .getter(d -> Optional.ofNullable(d.expiresIn()).map(Instant::getEpochSecond).orElse(null))
                        .setter((builder, value) -> builder.expiresIn(Instant.ofEpochSecond(value))))
                .build();
    }
}
