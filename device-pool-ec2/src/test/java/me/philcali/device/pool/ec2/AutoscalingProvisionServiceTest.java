package me.philcali.device.pool.ec2;

import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ProvisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class AutoscalingProvisionServiceTest {
    @Mock
    private AutoScalingClient autoscaling;
    @Mock
    private Ec2Client ec2;

    private ProvisionService service;
    private final String groupName = "TestGroup";

    @BeforeEach
    void setup() {
        service = AutoscalingProvisionService.builder()
                .autoscalingGroupName(groupName)
                .autoscaling(autoscaling)
                .ec2(ec2)
                .build();
    }

    private List<String> describeGroupWithInstances(final AutoScalingGroup...responses) {
        DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder()
                .autoScalingGroupNames(groupName)
                .build();

        AtomicInteger numberOfTimesCalled = new AtomicInteger();
        when(autoscaling.describeAutoScalingGroups(eq(request))).then(answer ->
                DescribeAutoScalingGroupsResponse.builder()
                        .autoScalingGroups(responses[numberOfTimesCalled.getAndIncrement()])
                        .build());

        return Arrays.stream(responses)
                .flatMap(response -> response.instances().stream())
                .map(Instance::instanceId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Test
    void GIVEN_provision_service_is_created_WHEN_provision_THEN_group_is_detached() {
        List<String> instanceIds = describeGroupWithInstances(
                AutoScalingGroup.builder()
                        .autoScalingGroupName(groupName)
                        .desiredCapacity(1)
                        .instances(Instance.builder()
                                .instanceId("i-abcedfgabc")
                                .healthStatus("HEALTHY")
                                .lifecycleState(LifecycleState.IN_SERVICE)
                                .build())
                        .build());

        assertEquals(1, instanceIds.size());

        ProvisionOutput expectedOutput = ProvisionOutput.builder()
                .id("abc-efg")
                .addReservations(Reservation.builder()
                        .deviceId(instanceIds.get(0))
                        .status(Status.SUCCEEDED)
                        .build())
                .build();

        ProvisionOutput output = service.provision(ProvisionInput.builder()
                .id("abc-efg")
                .amount(instanceIds.size())
                .build());

        assertEquals(expectedOutput, output);

        verify(autoscaling).detachInstances(eq(DetachInstancesRequest.builder()
                .autoScalingGroupName(groupName)
                .instanceIds(instanceIds.get(0))
                .build()));
    }

    @Test
    void GIVEN_provision_service_is_created_WHEN_provision_is_greater_THEN_group_is_detached() {
        ProvisionInput input = ProvisionInput.builder()
                .id("this-test-is-something-else")
                .amount(3)
                .build();

        List<Instance> instances = Arrays.asList(
                Instance.builder()
                        .instanceId("i-abcedfgabc")
                        .healthStatus("HEALTHY")
                        .lifecycleState(LifecycleState.IN_SERVICE)
                        .build(),
                Instance.builder()
                        .instanceId("i-defdefdef")
                        .healthStatus("HEALTHY")
                        .lifecycleState(LifecycleState.PENDING)
                        .build(),
                Instance.builder()
                        .instanceId("i-hijhijhij")
                        .healthStatus("HEALTHY")
                        .lifecycleState(LifecycleState.PENDING)
                        .build()
        );
        List<String> instanceIds = describeGroupWithInstances(
                AutoScalingGroup.builder()
                        .autoScalingGroupName(groupName)
                        .desiredCapacity(1)
                        .instances(instances.get(0))
                        .build(),
                AutoScalingGroup.builder()
                        .autoScalingGroupName(groupName)
                        .desiredCapacity(3)
                        .instances(instances.subList(0, 2))
                        .build(),
                AutoScalingGroup.builder()
                        .autoScalingGroupName(groupName)
                        .desiredCapacity(3)
                        .instances(instances)
                        .build()
        );

        ProvisionOutput output = service.provision(input);

        verify(autoscaling).setDesiredCapacity(eq(SetDesiredCapacityRequest.builder()
                .desiredCapacity(3)
                .autoScalingGroupName(groupName)
                .build()));

        verify(autoscaling).setDesiredCapacity(eq(SetDesiredCapacityRequest.builder()
                .desiredCapacity(1)
                .autoScalingGroupName(groupName)
                .build()));

        ProvisionOutput expectedOutput = ProvisionOutput.builder()
                .id(input.id())
                .addReservations(
                        Reservation.builder()
                                .deviceId(instanceIds.get(0))
                                .status(Status.SUCCEEDED)
                                .build(),
                        Reservation.builder()
                                .deviceId(instanceIds.get(1))
                                .status(Status.PROVISIONING)
                                .build(),
                        Reservation.builder()
                                .deviceId(instanceIds.get(2))
                                .status(Status.PROVISIONING)
                                .build()
                )
                .build();

        assertEquals(expectedOutput, output);

        verify(autoscaling).detachInstances(eq(DetachInstancesRequest.builder()
                .autoScalingGroupName(groupName)
                .instanceIds(instanceIds)
                .build()));
    }
}
