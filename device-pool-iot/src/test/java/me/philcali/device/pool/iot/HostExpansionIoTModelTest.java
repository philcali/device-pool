package me.philcali.device.pool.iot;

import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.provision.ExpandingHostProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsInThingGroupRequest;
import software.amazon.awssdk.services.iot.model.ListThingsInThingGroupResponse;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class HostExpansionIoTModelTest {
    @Mock
    IotClient iot;

    private HostExpansionIoT hostExpansion;

    @BeforeEach
    void setUp() {
        hostExpansion = HostExpansionIoT.builder()
                .iot(iot)
                .thingGroup("ThingGroup")
                .build();
    }

    @Test
    void GIVEN_iot_host_expansion_WHEN_requesting_hosts_THEN_new_page_is_delivered() {
        ListThingsInThingGroupRequest request = ListThingsInThingGroupRequest.builder()
                .nextToken("abc-123")
                .maxResults(20)
                .recursive(true)
                .thingGroupName("ThingGroup")
                .build();
        ListThingsInThingGroupResponse response = ListThingsInThingGroupResponse.builder()
                .nextToken("bbb-222")
                .things("ThingA", "ThingB", "ThingC")
                .build();
        doReturn(response).when(iot).listThingsInThingGroup(eq(request));
        ExpandingHostProvider.NextSetHosts hosts = hostExpansion.apply("abc-123", 20);

        List<Host> expected = Arrays.asList(
                Host.builder().hostName("ThingA").deviceId("ThingA").platform(hostExpansion.platform()).build(),
                Host.builder().hostName("ThingB").deviceId("ThingB").platform(hostExpansion.platform()).build(),
                Host.builder().hostName("ThingC").deviceId("ThingC").platform(hostExpansion.platform()).build()
        );
        assertEquals("bbb-222", hosts.nextToken());
        assertEquals(expected, hosts.hosts());
    }
}
