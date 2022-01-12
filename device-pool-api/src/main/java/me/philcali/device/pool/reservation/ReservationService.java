package me.philcali.device.pool.reservation;

import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.Reservation;

public interface ReservationService extends AutoCloseable {
    Host exchange(Reservation reservation) throws ReservationException;
}
