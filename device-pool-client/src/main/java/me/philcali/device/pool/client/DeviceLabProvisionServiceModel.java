/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.client;

import me.philcali.device.pool.client.exception.DeviceLabServiceException;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Reservation;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ProvisionService;
import me.philcali.device.pool.reservation.ReservationService;
import me.philcali.device.pool.service.api.ObjectRepository;
import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.DeviceObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.QueryParams;
import me.philcali.device.pool.service.api.model.QueryResults;
import me.philcali.device.pool.service.api.model.ReservationObject;
import me.philcali.device.pool.service.client.DeviceLabService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApiModel
@Value.Immutable
abstract class DeviceLabProvisionServiceModel implements ProvisionService, ReservationService {
    private static final Logger LOGGER = LogManager.getLogger(DeviceLabProvisionService.class);
    private final Set<String> inflightProvisions = ConcurrentHashMap.newKeySet();

    @Value.Default
    DeviceLabService deviceLabService() {
        return DeviceLabService.create();
    }

    abstract PlatformOS platform();

    @Value.Default
    Function<DeviceObject, Host.Builder> hostConvert() {
        return device -> Host.builder()
                .deviceId(device.id())
                .platform(platform())
                .hostName(device.publicAddress());
    }

    abstract String poolId();

    <T> T safelyCall(
            Call<T> result,
            Function<Exception, RuntimeException> translateException,
            boolean swallowException) {
        try {
            Response<T> response = result.execute();
            if (response.isSuccessful()) {
                LOGGER.debug("Successful response on {}: {}", result.request().url(), response.code());
                return response.body();
            } else if (!swallowException) {
                throw translateException.apply(new DeviceLabServiceException(response.message(), response.code()));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to request {}:", result.request().url(), e);
            if (!swallowException) {
                throw translateException.apply(e);
            }
        }
        return null;
    }

    private Reservation convert(ReservationObject object) {
        return Reservation.builder()
                .deviceId(object.deviceId())
                .status(object.status())
                .build();
    }

    private ProvisionOutput convert(ProvisionObject object) {
        ProvisionOutput.Builder builder = ProvisionOutput.builder()
                .id(object.id())
                .status(object.status());
        QueryResults<ReservationObject> queryResults = null;
        do {
            Call<QueryResults<ReservationObject>> result = deviceLabService().listReservations(
                    poolId(),
                    object.id(),
                    QueryParams.builder()
                            .nextToken(Optional.ofNullable(queryResults).map(QueryResults::nextToken).orElse(null))
                            .limit(ObjectRepository.MAX_ITEMS)
                            .build());
            queryResults = safelyCall(result, ProvisioningException::new, false);
            builder.addAllReservations(queryResults.results().stream()
                    .map(this::convert)
                    .collect(Collectors.toList()));
        } while (queryResults.isTruncated());
        return builder.build();
    }

    @Override
    public ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException {
        Call<ProvisionObject> result = deviceLabService().getProvision(poolId(), output.id());
        ProvisionObject provisionObject = safelyCall(result, ProvisioningException::new, false);
        if (provisionObject.status().isTerminal()) {
            inflightProvisions.remove(provisionObject.id());
        }
        return convert(provisionObject);
    }

    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        Call<ProvisionObject> response = deviceLabService().createProvision(poolId(), CreateProvisionObject.builder()
                .amount(input.amount())
                .id(input.id())
                .build());
        ProvisionObject provisionObject = safelyCall(response, ProvisioningException::new, false);
        if (provisionObject.status().equals(Status.REQUESTED)) {
            inflightProvisions.add(provisionObject.id());
        }
        return ProvisionOutput.builder()
                .id(provisionObject.id())
                .status(provisionObject.status())
                .succeeded(false)
                .build();
    }

    @Override
    public Host exchange(Reservation reservation) throws ReservationException {
        Call<DeviceObject> result = deviceLabService().getDevice(poolId(), reservation.deviceId());
        DeviceObject device = safelyCall(result, ReservationException::new, false);
        return hostConvert().andThen(Host.Builder::build).apply(device);
    }

    @Override
    public void close() throws Exception {
        inflightProvisions.forEach(provisionId -> {
            Call<ProvisionObject> result = deviceLabService().cancelProvision(poolId(), provisionId);
            safelyCall(result, ProvisioningException::new, true);
        });
        inflightProvisions.clear();
    }
}
