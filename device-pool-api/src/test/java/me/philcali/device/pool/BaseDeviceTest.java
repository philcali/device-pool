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
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.Host;
import me.philcali.device.pool.model.PlatformOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class BaseDeviceTest {
    @Mock
    private Connection connection;

    @Mock
    private ContentTransferAgent transfer;

    private Device device;
    private Host host;

    @BeforeEach
    void setup() {
        host = Host.builder()
                .deviceId("test-id")
                .platform(PlatformOS.of("Linux", "aarch64"))
                .hostName("myhost.amazon.com")
                .build();

        device = BaseDevice.builder()
                .host(host)
                .connection(connection)
                .contentTransfer(transfer)
                .build();
    }

    @Test
    void GIVEN_device_is_created_WHEN_executing_commands_THEN_connection_is_used() {
        CommandInput input = CommandInput.of("echo Hello World");
        CommandOutput output = CommandOutput.builder()
                .stdout("Hello World".getBytes(StandardCharsets.UTF_8))
                .originalInput(input)
                .exitCode(0)
                .build();

        when(connection.execute(eq(input))).thenReturn(output);

        assertEquals(output, device.execute(input));
    }

    @Test
    void GIVEN_device_is_created_WHEN_execution_fails_THEN_wrapped_exception_is_thrown() {
        CommandInput input = CommandInput.of("echo Hello World");
        when(connection.execute(eq(input))).thenThrow(ConnectionException.class);
        assertThrows(DeviceInteractionException.class, () -> device.execute(input));
    }

    @Test
    void GIVEN_device_is_created_WHEN_copying_to_THEN_transfer_is_invoked() {
        CopyInput input = CopyInput.of("/input/source", "/input/destination");
        device.copyTo(input);
        verify(transfer).send(eq(input));
    }

    @Test
    void GIVEN_device_is_created_WHEN_copying_from_THEN_transfer_is_invoked() {
        CopyInput input = CopyInput.of("/input/source", "/input/destination");
        device.copyFrom(input);
        verify(transfer).receive(eq(input));
    }

    @Test
    void GIVEN_device_is_created_WHEN_copying_to_fails_THEN_wrapped_exception_is_thrown() {
        CopyInput input = CopyInput.of("/input/source", "/input/destination");
        doThrow(ContentTransferException.class).when(transfer).send(eq(input));
        assertThrows(DeviceInteractionException.class, () -> device.copyTo(input));
    }

    @Test
    void GIVEN_device_is_created_WHEN_copying_from_fails_THEN_wrapped_exception_is_thrown() {
        CopyInput input = CopyInput.of("/input/source", "/input/destination");
        doThrow(ContentTransferException.class).when(transfer).receive(eq(input));
        assertThrows(DeviceInteractionException.class, () -> device.copyFrom(input));
    }

    @Test
    void GIVEN_device_is_created_WHEN_closing_THEN_underlying_closeables_are_closed() throws Exception {
        device.close();

        verify(connection).close();
        verify(transfer).close();
    }
}
