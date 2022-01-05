package me.philcali.device.pool.service.api;

import me.philcali.device.pool.service.api.model.CreateReservationObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.api.model.UpdateReservationObject;

public interface ReservationRepo
        extends ObjectRepository<ReservationObject, CreateReservationObject, UpdateReservationObject> {
}
