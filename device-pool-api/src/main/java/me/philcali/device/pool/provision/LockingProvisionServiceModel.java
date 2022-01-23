/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.provision;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.lock.LockingService;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.LockInput;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;

import java.util.function.Function;

@ApiModel
@Value.Immutable
abstract class LockingProvisionServiceModel implements ProvisionService {
    private static final Logger LOGGER = LogManager.getLogger(LockingProvisionService.class);

    abstract ProvisionService provisionService();

    abstract LockingService lockingService();

    @Value.Default
    Function<ProvisionInput, LockInput> lockingFunction() {
        return input -> LockInput.of(input.id());
    }

    @Override
    public ProvisionOutput provision(ProvisionInput input) throws ProvisioningException {
        try (LockingService.Lock lock = lockingService().tryAcquire(lockingFunction().apply(input))) {
            LOGGER.info("Obtained a lock for {}: {}", input, lock.lockInput());
            return provisionService().provision(input);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ProvisioningException("Interrupted while provisioning: " + input.id());
        }
    }

    @Override
    public ProvisionOutput describe(ProvisionOutput provisionOutput) throws ProvisioningException {
        return provisionService().describe(provisionOutput);
    }

    @Override
    public void close() throws Exception {
        provisionService().close();
    }
}
