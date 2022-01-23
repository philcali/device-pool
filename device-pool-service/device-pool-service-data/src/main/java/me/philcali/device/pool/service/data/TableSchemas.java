/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data;

import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.DeviceLockObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.DevicePoolEndpoint;
import me.philcali.device.pool.service.api.model.DevicePoolEndpointType;
import me.philcali.device.pool.service.api.model.DevicePoolLockOptions;
import me.philcali.device.pool.service.api.model.DevicePoolObject;
import me.philcali.device.pool.service.api.model.DevicePoolType;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
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

    private static TableSchema<DevicePoolEndpoint> endpoint() {
        return StaticImmutableTableSchema.builder(DevicePoolEndpoint.class, DevicePoolEndpoint.Builder.class)
                .newItemBuilder(DevicePoolEndpoint::builder, DevicePoolEndpoint.Builder::build)
                .addAttribute(String.class, a -> a.name("uri")
                        .getter(DevicePoolEndpoint::uri)
                        .setter(DevicePoolEndpoint.Builder::uri))
                .addAttribute(DevicePoolEndpointType.class, a -> a.name("type")
                        .getter(DevicePoolEndpoint::type)
                        .setter(DevicePoolEndpoint.Builder::type)
                        .attributeConverter(EnumAttributeConverter.create(DevicePoolEndpointType.class)))
                .build();
    }

    private static TableSchema<DevicePoolLockOptions> lockOptions() {
        return StaticImmutableTableSchema.builder(DevicePoolLockOptions.class, DevicePoolLockOptions.Builder.class)
                .newItemBuilder(DevicePoolLockOptions::builder, DevicePoolLockOptions.Builder::build)
                .addAttribute(Boolean.class, a -> a.name("enabled")
                        .getter(DevicePoolLockOptions::enabled)
                        .setter(DevicePoolLockOptions.Builder::enabled))
                .addAttribute(Long.class, a -> a.name("initialDuration")
                        .getter(DevicePoolLockOptions::initialDuration)
                        .setter(DevicePoolLockOptions.Builder::initialDuration))
                .build();
    }

    public static TableSchema<DevicePoolObject> poolTableSchema() {
        return commonTable(DevicePoolObject.class, DevicePoolObject.Builder.class,
                DevicePoolObject::key, DevicePoolObject.Builder::key,
                DevicePoolObject::name, DevicePoolObject.Builder::name)
                .newItemBuilder(DevicePoolObject::builder, DevicePoolObject.Builder::build)
                .addAttribute(DevicePoolType.class, a -> a.name("type")
                        .getter(DevicePoolObject::type)
                        .setter(DevicePoolObject.Builder::type)
                        .attributeConverter(EnumAttributeConverter.create(DevicePoolType.class)))
                .addAttribute(EnhancedType.documentOf(DevicePoolEndpoint.class, endpoint()), a -> a.name("endpoint")
                        .getter(DevicePoolObject::endpoint)
                        .setter(DevicePoolObject.Builder::endpoint))
                .addAttribute(EnhancedType.documentOf(DevicePoolLockOptions.class, lockOptions()), a -> a.name("lockOptions")
                        .getter(DevicePoolObject::lockOptions)
                        .setter(DevicePoolObject.Builder::lockOptions))
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
                .addAttribute(Integer.class, a -> a.name("amount")
                        .getter(ProvisionObject::amount)
                        .setter(ProvisionObject.Builder::amount))
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
                .newItemBuilder(ReservationObject::builder, ReservationObject.Builder::build)
                .addAttribute(String.class, a -> a.name("deviceId")
                        .getter(ReservationObject::deviceId)
                        .setter(ReservationObject.Builder::deviceId))
                .addAttribute(Status.class, a -> a.name("status")
                        .getter(ReservationObject::status)
                        .setter(ReservationObject.Builder::status)
                        .attributeConverter(EnumAttributeConverter.create(Status.class)))
                .addAttribute(String.class, a -> a.name("message")
                        .getter(ReservationObject::message)
                        .setter(ReservationObject.Builder::message))
                .addAttribute(Long.class, a -> a.name("createdAt")
                        .getter(p -> Optional.ofNullable(p.createdAt()).map(Instant::getEpochSecond).orElse(null))
                        .setter((builder, value) -> builder.createdAt(Instant.ofEpochSecond(value))))
                .addAttribute(Long.class, a -> a.name("updatedAt")
                        .getter(p -> p.updatedAt().getEpochSecond())
                        .setter((builder, value) -> builder.updatedAt(Instant.ofEpochSecond(value))))
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

    public static TableSchema<DeviceLockObject> deviceLockSchema() {
        return commonTable(DeviceLockObject.class, DeviceLockObject.Builder.class,
                DeviceLockObject::key, DeviceLockObject.Builder::key,
                DeviceLockObject::id, DeviceLockObject.Builder::id)
                .newItemBuilder(DeviceLockObject::builder, DeviceLockObject.Builder::build)
                .addAttribute(String.class, a -> a.name("provisionId")
                        .getter(DeviceLockObject::provisionId)
                        .setter(DeviceLockObject.Builder::provisionId))
                .addAttribute(String.class, a -> a.name("reservationId")
                        .getter(DeviceLockObject::reservationId)
                        .setter(DeviceLockObject.Builder::reservationId))
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
