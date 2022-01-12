package me.philcali.device.pool.ec2;

import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.ApiModel;
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

@ApiModel
@Value.Immutable
abstract class Ec2ReservationServiceModel implements ReservationService {
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

    private Host convertHost(Instance instance) {
        return Host.builder()
                .deviceId(instance.instanceId())
                .hostName(hostAddress().apply(instance))
                .port(port())
                .platform(hostPlatform().apply(instance))
                .proxyJump(proxyJump())
                .build();
    }

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

    @Override
    public void close() throws Exception {
        ec2().close();
    }
}
