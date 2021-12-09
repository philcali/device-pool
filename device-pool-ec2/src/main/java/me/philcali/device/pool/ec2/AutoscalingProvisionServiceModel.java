package me.philcali.device.pool.ec2;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ProvisionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.InstanceHealthStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApiModel
@Value.Immutable
abstract class AutoscalingProvisionServiceModel implements ProvisionService {
    private static final Logger LOGGER = LogManager.getLogger(AutoscalingProvisionService.class);
    private static final int RUNNING = 16;
    private static final int PENDING = 0;
    private static final int LOW_BITS = 265;

    @Value.Default
    AutoScalingClient autoscaling() {
        return AutoScalingClient.create();
    }

    abstract String autoscalingGroupName();

    @Value.Default
    Ec2Client ec2() {
        return Ec2Client.create();
    }

    @Value.Default
    WaiterOverrideConfiguration overrideConfiguration() {
        return WaiterOverrideConfiguration.builder()
                .waitTimeout(Duration.ofSeconds(30))
                .maxAttempts(10)
                .build();
    }

    private AutoScalingGroup describeGroupOrThrow() {
        try {
            return autoscaling().describeAutoScalingGroupsPaginator(DescribeAutoScalingGroupsRequest.builder()
                            .autoScalingGroupNames(autoscalingGroupName())
                            .build())
                    .autoScalingGroups()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ProvisioningException("Could not find group: " + autoscalingGroupName()));
        } catch (AutoScalingException e) {
            throw new ProvisioningException(e);
        }
    }

    private Predicate<DescribeAutoScalingGroupsResponse> isRequiredCapacity(int required) {
        return response -> response.autoScalingGroups().stream()
                .filter(group -> group.autoScalingGroupName().equals(autoscalingGroupName()))
                .flatMap(group -> group.instances().stream())
                .filter(this::isNotDetached)
                .count() >= required;
    }

    private Reservation translateInstance(String instanceId, LifecycleState state) {
        Status status;
        switch (state) {
            case WARMED_RUNNING:
            case IN_SERVICE:
                status = Status.SUCCEEDED;
                break;
            case WARMED_STOPPED:
            case QUARANTINED:
            case TERMINATED:
            case TERMINATING:
            case TERMINATING_PROCEED:
            case TERMINATING_WAIT:
            case WARMED_TERMINATED:
            case WARMED_TERMINATING:
            case WARMED_TERMINATING_PROCEED:
            case WARMED_TERMINATING_WAIT:
                status = Status.FAILED;
                break;
            default:
                status = Status.PROVISIONING;
        }
        return Reservation.builder()
                .status(status)
                .deviceId(instanceId)
                .build();
    }

    private Reservation translateInstance(String instanceId, int code) {
        Status status;
        switch (code) {
            case PENDING:
                status = Status.PROVISIONING;
                break;
            case RUNNING:
                status = Status.SUCCEEDED;
                break;
            default:
                status = code <= LOW_BITS ? Status.FAILED : Status.REQUESTED;
        }
        return Reservation.builder()
                .deviceId(instanceId)
                .status(status)
                .build();
    }

    private boolean isNotDetached(Instance instance) {
        return !(instance.lifecycleState().equals(LifecycleState.DETACHED)
                || instance.lifecycleState().equals(LifecycleState.DETACHING))
                && instance.healthStatus().equals(InstanceHealthStatus.HEALTHY.name());
    }

    private Waiter<DescribeAutoScalingGroupsResponse> waiterForCapacity(int requiredCapacity) {
        Predicate<DescribeAutoScalingGroupsResponse> responsePredicate = isRequiredCapacity(requiredCapacity);
        return Waiter.builder(DescribeAutoScalingGroupsResponse.class)
                .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(responsePredicate.negate()))
                .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(responsePredicate))
                .build();
    }

    private ProvisionOutput validInstancesToReservations(ProvisionInput input, List<Instance> validInstances) {
        List<Reservation> reservations = new ArrayList<>();
        List<String> instanceIds = new ArrayList<>();
        // Pull current instances off of group
        validInstances.stream().limit(input.amount()).forEach(instance -> {
            reservations.add(translateInstance(instance.instanceId(), instance.lifecycleState()));
            instanceIds.add(instance.instanceId());
        });

        try {
            DetachInstancesRequest detach = DetachInstancesRequest.builder()
                    .instanceIds(instanceIds)
                    .autoScalingGroupName(autoscalingGroupName())
                    .build();
            autoscaling().detachInstances(detach);
        } catch (AutoScalingException e) {
            throw new ProvisioningException(e);
        }

        return ProvisionOutput.builder()
                .id(input.id())
                .reservations(reservations)
                .build();
    }

    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        AutoScalingGroup group = describeGroupOrThrow();
        List<Instance> validInstances = group.instances().stream()
                .filter(this::isNotDetached)
                .peek(instance -> LOGGER.debug("Group {} detected {}: {}",
                        autoscalingGroupName(),
                        instance.instanceId(),
                        instance.lifecycleState()))
                .collect(Collectors.toList());
        if (validInstances.size() < input.amount()) {
            int remainder = input.amount() - validInstances.size();
            int required = group.desiredCapacity() + remainder;
            try {
                autoscaling().setDesiredCapacity(SetDesiredCapacityRequest.builder()
                        .autoScalingGroupName(autoscalingGroupName())
                        .desiredCapacity(remainder)
                        .build());
                LOGGER.info("Upgrading capacity to fulfill the {}, previous desired: {}, new desired{}",
                        input.id(), group.desiredCapacity(), required);

                Waiter<DescribeAutoScalingGroupsResponse> waiter = waiterForCapacity(required);
                WaiterResponse<DescribeAutoScalingGroupsResponse> response = waiter.run(() -> autoscaling()
                        .describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder()
                                .autoScalingGroupNames(autoscalingGroupName())
                                .build()), overrideConfiguration());

                ResponseOrException<DescribeAutoScalingGroupsResponse> description = response.matched();
                description.exception().ifPresent(ProvisioningException::new);
                LOGGER.info("Updated to required capacity for {}", input);
                return validInstancesToReservations(input, description.response()
                        .orElseThrow(() -> new ProvisioningException("Could not update capacity"))
                        .autoScalingGroups()
                        .stream()
                        .filter(g -> g.autoScalingGroupName().equals(autoscalingGroupName()))
                        .flatMap(g -> g.instances().stream())
                        .filter(this::isNotDetached)
                        .collect(Collectors.toList()));
            } catch (SdkClientException e) {
                throw new ProvisioningException(e);
            } finally {
                LOGGER.info("Reset capacity for {} to {}", autoscalingGroupName(), group.desiredCapacity());
                autoscaling().setDesiredCapacity(SetDesiredCapacityRequest.builder()
                        .autoScalingGroupName(autoscalingGroupName())
                        .desiredCapacity(group.desiredCapacity())
                        .build());
            }
        } else {
            return validInstancesToReservations(input, validInstances);
        }
    }

    @Override
    public ProvisionOutput describe(ProvisionOutput provisionOutput) throws ProvisioningException {
        try {
            DescribeInstancesResponse response = ec2().describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(provisionOutput.reservations().stream()
                            .map(Reservation::deviceId)
                            .collect(Collectors.toList()))
                    .build());
            return ProvisionOutput.builder()
                    .from(provisionOutput)
                    .reservations(response.reservations().stream()
                            .flatMap(reservation -> reservation.instances().stream())
                            .map(instance -> translateInstance(instance.instanceId(), instance.state().code()))
                            .collect(Collectors.toList()))
                    .build();
        } catch (Ec2Exception e) {
            throw new ProvisioningException(e);
        }
    }
}
