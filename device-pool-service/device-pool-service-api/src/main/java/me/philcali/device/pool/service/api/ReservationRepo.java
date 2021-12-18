package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.exception.NotFoundException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.ReservationObject;

public interface ReservationRepo {
    ReservationObject get(CompositeKey key, String reservationId) throws NotFoundException;

    QueryResults<ReservationObject> list(CompositeKey key, QueryParams params);

    void delete(CompositeKey key, String reservationId);
}
