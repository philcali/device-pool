/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CopyInput;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
abstract class AWSCLIAgentCommandModel implements AgentCommand {
    /**
     * <p>pathToBinary.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Value.Default
    public String pathToBinary() {
        return "";
    }

    /**
     * <p>binary.</p>
     *
     * @return a {@link java.lang.String} object
     */
    @Value.Default
    public String binary() {
        return "aws";
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link me.philcali.device.pool.s3.AWSCLIAgentCommand} object
     */
    public static AWSCLIAgentCommand create() {
        return AWSCLIAgentCommand.builder().build();
    }

    /** {@inheritDoc} */
    @Override
    public CommandInput copy(CopyInput input) {
        return CommandInput.builder()
                .line(pathToBinary() + binary())
                .addArgs("s3", "cp", input.source(), input.destination())
                .build();
    }
}
