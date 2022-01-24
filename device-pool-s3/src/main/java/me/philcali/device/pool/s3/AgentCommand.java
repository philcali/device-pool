/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CopyInput;

public interface AgentCommand {
    CommandInput copy(CopyInput input);
}
