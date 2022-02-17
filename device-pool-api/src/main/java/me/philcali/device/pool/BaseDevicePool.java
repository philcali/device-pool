/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.model.Status;
import me.philcali.device.pool.provision.ProvisionService;
import me.philcali.device.pool.reservation.ReservationService;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link me.philcali.device.pool.BaseDevicePool} implements the {@link me.philcali.device.pool.DevicePool} breaking down the control plane
 * implementation into distinct components for flexible injection. Some components might represent
 * both the {@link me.philcali.device.pool.reservation.ReservationService} and {@link me.philcali.device.pool.provision.ProvisionService} control plane functions, for which
 * they can be set simultaneously via the {@link me.philcali.device.pool.BaseDevicePool.Builder}. The same is also true with {@link me.philcali.device.pool.connection.ConnectionFactory}
 * and {@link me.philcali.device.pool.content.ContentTransferAgentFactory} for generating {@link me.philcali.device.pool.Device}. The {@link me.philcali.device.pool.BaseDevicePool}
 * implements the <code>obtain</code> method by exchanging complete {@link me.philcali.device.pool.model.Reservation}
 * detail for {@link me.philcali.device.pool.model.Host} data path information to be used in generated {@link me.philcali.device.pool.connection.Connection} and
 * {@link me.philcali.device.pool.content.ContentTransferAgent}. The {@link me.philcali.device.pool.Device} implementation is a {@link me.philcali.device.pool.BaseDevice}.
 *
 * @author philcali
 * @version $Id: $Id
 */
@APIShadowModel
@Value.Immutable
public abstract class BaseDevicePool implements DevicePool {
    abstract ProvisionService provisionService();

    abstract ReservationService reservationService();

    abstract ConnectionFactory connections();

    abstract ContentTransferAgentFactory transfers();

    public static class Builder extends ImmutableBaseDevicePool.Builder {
        public final <T extends ProvisionService & ReservationService> Builder provisionAndReservationService(
                T service) {
            return provisionService(service).reservationService(service);
        }

        public final <T extends ConnectionFactory & ContentTransferAgentFactory> Builder connectionAndContentFactory(
                T factory) {
            return connections(factory).transfers(factory);
        }
    }

    /**
     * <p>builder.</p>
     *
     * @return a {@link me.philcali.device.pool.BaseDevicePool.Builder} object
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@inheritDoc} */
    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        return provisionService().provision(input);
    }

    /** {@inheritDoc} */
    @Override
    public ProvisionOutput describe(ProvisionOutput provisionOutput) throws ProvisioningException {
        return provisionService().describe(provisionOutput);
    }

    /** {@inheritDoc} */
    @Override
    public List<Device> obtain(ProvisionOutput output) throws ProvisioningException {
        try {
            return output.reservations().stream()
                    .filter(reservation -> reservation.status().equals(Status.SUCCEEDED))
                    .map(reservation -> {
                        final Host host = reservationService().exchange(reservation);
                        final Connection connection = connections().connect(host);
                        final ContentTransferAgent agent = transfers().connect(output.id(), connection, host);
                        return BaseDevice.builder()
                                .connection(connection)
                                .contentTransfer(agent)
                                .host(host)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (ReservationException
                | ConnectionException
                | ContentTransferException e) {
            throw new ProvisioningException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        SafeClosable.safelyClose(provisionService(), reservationService(), transfers(), connections());
    }
}
