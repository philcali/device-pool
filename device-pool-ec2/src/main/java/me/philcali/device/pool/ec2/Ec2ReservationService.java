/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ec2;

import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.reservation.ReservationService;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * A {@link me.philcali.device.pool.reservation.ReservationService} implemented by {@link me.philcali.device.pool.ec2.Ec2ReservationService} using AWS EC2.
 * The implementation simply pumps EC2 instance reservations as discreet {@link me.philcali.device.pool.Device}
 * reservation. The {@link me.philcali.device.pool.ec2.Ec2ReservationService} relies on the client to hint at the {@link me.philcali.device.pool.model.PlatformOS} that
 * makes up the reservations.
 */
@APIShadowModel
@Value.Immutable
public abstract class Ec2ReservationService implements ReservationService {
    @Value.Default
    Ec2Client ec2() {
        return Ec2Client.create();
    }

    abstract PlatformOS platformOS();

    @Nullable
    abstract String proxyJump();

    @Value.Default
    int port() {
        return 22;
    }

    @Value.Default
    Function<Instance, PlatformOS> hostPlatform() {
        return instance -> platformOS();
    }

    @Value.Default
    Function<Instance, String> hostAddress() {
        return Instance::publicIpAddress;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Ec2ReservationService of(PlatformOS platformOS) {
        return builder().platformOS(platformOS).build();
    }

    public static final class Builder
            extends ImmutableEc2ReservationService.Builder
            implements ConfigBuilder<Ec2ReservationService> {
        @Override
        public Ec2ReservationService fromConfig(DevicePoolConfig config) {
            return config.namespace("reservation.ec2")
                    .flatMap(entry -> {
                        entry.get("port").map(Integer::parseInt).ifPresent(this::port);
                        entry.get("proxy").ifPresent(this::proxyJump);
                        entry.get("usePrivateAddress")
                                .filter(Boolean::parseBoolean)
                                .ifPresent(usePrivate -> hostAddress(Instance::privateIpAddress));
                        return entry.get("platform")
                                .map(PlatformOS::fromString)
                                .map(this::platformOS)
                                .map(ImmutableEc2ReservationService.Builder::build);
                    })
                    .orElseThrow(() -> new ReservationException("The ec2 reservation requires a platform property"));
        }
    }

    private Host convertHost(Instance instance) {
        return Host.builder()
                .deviceId(instance.instanceId())
                .hostName(hostAddress().apply(instance))
                .port(port())
                .platform(hostPlatform().apply(instance))
                .proxyJump(proxyJump())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public Host exchange(final Reservation reservation) throws ReservationException {
        try {
            DescribeInstancesResponse response = ec2().describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(reservation.deviceId())
                    .build());
            return response.reservations().stream()
                    .flatMap(r -> r.instances().stream())
                    .findFirst()
                    .map(this::convertHost)
                    .orElseThrow(() -> new ReservationException("Could not find " + reservation.deviceId()));
        } catch (Ec2Exception e) {
            throw new ReservationException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        ec2().close();
    }
}
