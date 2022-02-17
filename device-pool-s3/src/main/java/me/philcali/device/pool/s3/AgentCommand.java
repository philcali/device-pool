/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CopyInput;

/**
 * <p>AgentCommand interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface AgentCommand {
    /**
     * <p>copy.</p>
     *
     * @param input a {@link me.philcali.device.pool.model.CopyInput} object
     * @return a {@link me.philcali.device.pool.model.CommandInput} object
     */
    CommandInput copy(CopyInput input);
}
