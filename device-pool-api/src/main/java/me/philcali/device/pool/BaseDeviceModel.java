/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.exceptions.DeviceInteractionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.Host;
import org.immutables.value.Value;


@ApiModel
@Value.Immutable
abstract class BaseDeviceModel implements Device {
    abstract Host host();

    abstract Connection connection();

    abstract ContentTransferAgent contentTransfer();

    @Override
    public String id() {
        return host().deviceId();
    }

    @Override
    public CommandOutput execute(final CommandInput input) throws DeviceInteractionException {
        try {
            return connection().execute(input);
        } catch (ConnectionException exception) {
            throw new DeviceInteractionException(exception);
        }
    }

    @Override
    public void copyTo(final CopyInput input) throws DeviceInteractionException {
        try {
            contentTransfer().send(input);
        } catch (ContentTransferException e) {
            throw new DeviceInteractionException(e);
        }
    }

    @Override
    public void copyFrom(final CopyInput input) throws DeviceInteractionException {
        try {
            contentTransfer().receive(input);
        } catch (ContentTransferException e) {
            throw new DeviceInteractionException(e);
        }
    }

    @Override
    public void close() {
        SafeClosable.safelyClose(contentTransfer(), connection());
    }
}
