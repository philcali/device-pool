package me.philcali.device.pool.s3;

import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CopyInput;
import org.immutables.value.Value;

@ApiModel
@Value.Immutable
abstract class AWSCLIAgentCommandModel implements AgentCommand {
    @Value.Default
    public String pathToBinary() {
        return "";
    }

    @Value.Default
    public String binary() {
        return "aws";
    }

    public static AWSCLIAgentCommand create() {
        return AWSCLIAgentCommand.builder().build();
    }

    @Override
    public CommandInput copy(CopyInput input) {
        return CommandInput.builder()
                .line(pathToBinary() + binary())
                .addArgs("s3", "cp", input.source(), input.destination())
                .build();
    }
}
