/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;

public interface ProvisionService extends AutoCloseable {
    ProvisionOutput provision(ProvisionInput input) throws ProvisioningException;

    ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException;
}
