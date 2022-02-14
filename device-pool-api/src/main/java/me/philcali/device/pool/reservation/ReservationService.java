/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.reservation;

import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.Reservation;

/**
 * The injection point for describing data path details for a provision request.
 * The provisioning workflow will initialize reservation details for the intent
 * of establishing data paths. The {@link ReservationService} exchanges initialized
 * {@link Reservation} details for complete data paths. The data path is materialized
 * in the form of a {@link Host} to facilitate reachability in various data plane
 * components, namely the {@link me.philcali.device.pool.connection.Connection} and
 * {@link me.philcali.device.pool.content.ContentTransferAgent}.
 */
public interface ReservationService extends AutoCloseable {
    /**
     * The exchange of a {@link Reservation} manifest for a partial or
     * complete data path in the form of a {@link Host} entry.
     *
     * @param reservation The data path metadata in the form of a {@link Reservation}
     * @return The partial or complete data path details in the form of a {@link Host}
     * @throws ReservationException Failure to exchange {@link Reservation} metadata
     */
    Host exchange(Reservation reservation) throws ReservationException;
}
