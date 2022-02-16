/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.local;

import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalDevicePoolTest {
    private DevicePool pool;

    @BeforeEach
    void setUp() {
        pool = LocalDevicePool.create();
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void GIVEN_local_pool_is_created_WHEN_local_pool_provisions_THEN_devices_are_created() {
        ProvisionInput input = ProvisionInput.builder()
                .amount(5)
                .id(UUID.randomUUID().toString())
                .build();
        List<Device> devices = pool.provisionWait(input, 10, TimeUnit.SECONDS);
        List<String> hosts = Arrays.asList("host-1", "host-2", "host-3", "host-4", "host-5");
        assertEquals(5, devices.size());
        assertEquals(hosts, devices.stream().map(Device::id).collect(Collectors.toList()));

        assertThrows(ProvisioningException.class, () -> pool.describe(ProvisionOutput.of("nothing")));
    }
}
