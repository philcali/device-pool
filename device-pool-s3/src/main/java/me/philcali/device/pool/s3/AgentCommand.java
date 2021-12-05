package me.philcali.device.pool.s3;

import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CopyInput;

public interface AgentCommand {
    CommandInput copy(CopyInput input);
}
