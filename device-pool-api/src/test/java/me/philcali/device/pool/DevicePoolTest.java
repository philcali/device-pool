/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import me.philcali.device.pool.connection.NoopConnectionFactory;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.local.LocalDevicePool;
import me.philcali.device.pool.provision.LocalProvisionService;
import me.philcali.device.pool.reservation.NoopReservationService;
import me.philcali.device.pool.transfer.NoopTransferFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevicePoolTest {

    @Test
    void GIVEN_no_pool_WHEN_create_is_called_THEN_reads_local_properties() {
        DevicePool pool = DevicePool.create();
        assertEquals(LocalDevicePool.class, pool.getClass());
    }

    @Test
    void GIVEN_no_pool_WHEN_create_is_called_on_non_existing_THEN_failure_is_propagated() {
        assertThrows(ProvisioningException.class, () -> DevicePool.create(getClass()
                .getClassLoader().getResourceAsStream("Does Not exist")));
    }

    @Test
    void GIVEN_no_pool_WHEN_create_is_called_and_pool_does_not_exist_THEN_failure_is_propagated() {
        assertThrows(ProvisioningException.class, () -> DevicePool.create(getClass().getClassLoader()
                .getResourceAsStream("device/bad.properties")));
    }

    @Test
    void GIVEN_no_poo_WHEN_create_is_called_THEN_base_device_pool_is_created() {
        DevicePool pool = DevicePool.create(getClass().getClassLoader().getResourceAsStream("test/local.properties"));
        assertEquals(ImmutableBaseDevicePool.class, pool.getClass());

        BaseDevicePool baseDevicePool = (BaseDevicePool) pool;
        assertTrue(LocalProvisionService.class.isAssignableFrom(baseDevicePool.provisionService().getClass()));
        assertEquals(NoopReservationService.class, baseDevicePool.reservationService().getClass());
        assertEquals(NoopConnectionFactory.class, baseDevicePool.connections().getClass());
        assertEquals(NoopTransferFactory.class, baseDevicePool.transfers().getClass());
    }

    @Test
    void GIVEN_no_poo_WHEN_create_is_called_and_linked_functions_THEN_base_device_pool_is_created() {
        DevicePool pool = DevicePool.create(getClass().getClassLoader().getResourceAsStream("test/local2.properties"));
        assertEquals(ImmutableBaseDevicePool.class, pool.getClass());

        BaseDevicePool baseDevicePool = (BaseDevicePool) pool;
        assertTrue(LocalProvisionService.class.isAssignableFrom(baseDevicePool.provisionService().getClass()));
        assertEquals(baseDevicePool.provisionService(), baseDevicePool.reservationService());
        assertEquals(NoopConnectionFactory.class, baseDevicePool.connections().getClass());
        assertEquals(baseDevicePool.connections(), baseDevicePool.transfers());
    }

    @Test
    void GIVEN_no_poo_WHEN_create_is_called_and_not_exists_THEN_exception_is_thrown() {
        assertThrows(ProvisioningException.class, () -> DevicePool.create(getClass().getClassLoader().getResourceAsStream("test/local3.properties")));
    }
}
