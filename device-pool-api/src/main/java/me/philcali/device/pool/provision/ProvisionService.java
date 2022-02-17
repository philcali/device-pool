/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;

/**
 * The injection point for a {@link me.philcali.device.pool.DevicePool} specific to the
 * provisioning workflow or {@link me.philcali.device.pool.Device} acquisition. The
 * acquisition of {@link me.philcali.device.pool.Device} can be independent of a
 * reservation, which is to say: the intent to create data paths is decoupled from
 * the data paths established in the provisioning process. This is strictly control
 * plane interaction.
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface ProvisionService extends AutoCloseable {
    /**
     * Initializes the provisioning workflow with a {@link me.philcali.device.pool.model.ProvisionInput}.
     *
     * @param input The initial provision request in the form of a {@link me.philcali.device.pool.model.ProvisionInput}
     * @return The provision request handle in the form of a {@link me.philcali.device.pool.model.ProvisionOutput}
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure to create a provision request
     */
    ProvisionOutput provision(ProvisionInput input) throws ProvisioningException;

    /**
     * Describes the provision request details.
     *
     * @param output A partial or full {@link me.philcali.device.pool.model.ProvisionOutput}
     * @return A partial or complete {@link me.philcali.device.pool.model.ProvisionOutput}
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure to describe a provision request
     */
    ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException;
}
