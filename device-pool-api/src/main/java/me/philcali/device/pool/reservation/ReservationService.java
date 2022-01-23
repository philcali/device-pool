/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.reservation;

import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.Reservation;

public interface ReservationService extends AutoCloseable {
    Host exchange(Reservation reservation) throws ReservationException;
}
