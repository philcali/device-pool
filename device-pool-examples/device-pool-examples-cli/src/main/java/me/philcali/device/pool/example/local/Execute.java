/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.local;

import me.philcali.device.pool.example.Local;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.ProvisionInput;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Execute class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "exec",
        description = "Runs an arbitrary command on the devices"
)
public class Execute implements Runnable {
    @CommandLine.ParentCommand
    Local local;

    @CommandLine.Parameters(index = "0", description = "Command to run", paramLabel = "COMMANDS")
    String command;

    /** {@inheritDoc} */
    @Override
    public void run() {
        DevicePool pool = local.createPool();
        List<Device> devices = pool.provisionWait(ProvisionInput.builder()
                .amount(local.hostNames().size())
                .id("test-input")
                .build(), 10, TimeUnit.SECONDS);
        for (Device device : devices) {
            CommandOutput output = device.execute(CommandInput.of(command));
            System.out.println("Output from " + device.id());
            System.out.println(output.toUTF8String());
        }
    }
}
