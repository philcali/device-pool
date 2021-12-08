package me.philcali.device.pool.provision;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;

public interface ProvisionService {
    ProvisionOutput provision(ProvisionInput input) throws ProvisioningException;

    ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException;
}
