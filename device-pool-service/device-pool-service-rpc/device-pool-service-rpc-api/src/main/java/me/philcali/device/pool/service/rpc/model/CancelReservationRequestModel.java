package me.philcali.device.pool.service.rpc.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.ReservationObject;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
@JsonDeserialize(as = CancelReservationRequest.class)
abstract class CancelReservationRequestModel {
    abstract CompositeKey accountKey();

    abstract ProvisionObject provision();

    abstract ReservationObject reservation();
}
