package me.philcali.device.pool;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.exceptions.ReservationException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.provision.ProvisionService;
import me.philcali.device.pool.reservation.ReservationService;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

@ApiModel
@Value.Immutable
abstract class BaseDevicePoolModel implements DevicePool {
    abstract ProvisionService provisionService();

    abstract ReservationService reservationService();

    abstract ConnectionFactory connections();

    abstract ContentTransferAgentFactory transfers();

    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        return provisionService().provision(input);
    }

    @Override
    public ProvisionOutput describe(ProvisionOutput provisionOutput) throws ProvisioningException {
        return provisionService().describe(provisionOutput);
    }

    @Override
    public List<Device> obtain(ProvisionOutput output) throws ProvisioningException {
        try {
            return output.reservations().stream()
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
}
