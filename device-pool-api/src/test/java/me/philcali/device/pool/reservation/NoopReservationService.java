/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.reservation;

import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.Reservation;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoopReservationService implements ReservationService {
    @Override
    public Host exchange(Reservation reservation) throws ReservationException {
        return null;
    }

    @Override
    public void close() throws Exception {

    }

    public static final class Builder implements ConfigBuilder<NoopReservationService> {
        @Override
        public NoopReservationService fromConfig(DevicePoolConfig config) {
            Optional<DevicePoolConfig.DevicePoolConfigEntry> entry = config.namespace("reservation.noop");
            assertTrue(entry.isPresent(), "reservation.noop namespace not exist");
            assertEquals(Optional.of("value"), entry.flatMap(e -> e.get("test")));
            return new NoopReservationService();
        }
    }
}
