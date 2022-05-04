/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.lab;

import me.philcali.device.pool.example.Lab;
import me.philcali.device.pool.service.client.DeviceLabService;
import picocli.CommandLine;
import retrofit2.Response;

import java.io.IOException;
import java.util.Objects;

/**
 * <p>Destroy class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "destroy",
        description = "Destroys a device pool through the control plane",
        subcommands = {CommandLine.HelpCommand.class}
)
public class Destroy implements Runnable {
    @CommandLine.ParentCommand
    Lab deviceLab;

    /** {@inheritDoc} */
    @Override
    public void run() {
        DeviceLabService service = deviceLab.createService();
        try {
            Response<Void> response = service.deleteDevicePool(deviceLab.poolName()).execute();
            if (Objects.nonNull(response.errorBody())) {
                throw new RuntimeException(response.errorBody().string());
            }
            System.out.println("Successfully deleted " + deviceLab.poolName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
