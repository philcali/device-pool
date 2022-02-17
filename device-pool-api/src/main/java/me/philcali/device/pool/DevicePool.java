/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The client side abstraction for creating programmatic {@link me.philcali.device.pool.Device}s to interact with.
 * Implementations of the {@link me.philcali.device.pool.DevicePool} primarily consist of control plane functions to
 * obtain {@link me.philcali.device.pool.Device}s.
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface DevicePool extends AutoCloseable {
    /**
     * Creates a provision request for this {@link me.philcali.device.pool.DevicePool} implementation. It is
     * important to know that the provisioning process is asynchronous. Some implementations
     * may be able to fulfill the request immediately, and some may inform systems to acquire
     * the necessary resources. This method is considered the entrypoint of the provisioning
     * workflow.
     *
     * @param input Provision request to this pool in the form of a {@link me.philcali.device.pool.model.ProvisionInput}
     * @return The result of the provision request in the form of a {@link me.philcali.device.pool.model.ProvisionOutput}
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure to fulfill the provision request
     */
    ProvisionOutput provision(ProvisionInput input) throws ProvisioningException;

    /**
     * Describes the provisioning state of a {@link me.philcali.device.pool.model.ProvisionInput}.
     *
     * @param output A partial or complete {@link me.philcali.device.pool.model.ProvisionOutput} from a provision request
     * @return The result of the provision request in the form of a {@link me.philcali.device.pool.model.ProvisionOutput}
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure of describe the provision resource
     */
    ProvisionOutput describe(ProvisionOutput output) throws ProvisioningException;

    /**
     * Attempts to provide a collection of {@link me.philcali.device.pool.Device} attached to a provision request.
     *
     * @param output The state of a provision in the form of a {@link me.philcali.device.pool.model.ProvisionOutput}
     * @return A {@link java.util.List} of {@link me.philcali.device.pool.Device} entries
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure to obtain {@link me.philcali.device.pool.Device}s for this provision
     */
    List<Device> obtain(ProvisionOutput output) throws ProvisioningException;

    /**
     * Convenience method to block on any provision request. This method handles the polling of
     * a provision request. Example:
     * <br>
     * <code>
     *     <br>
     *     ProvisionInput input = ProvisionInput.builder().amount(5).build();
     *     <br>
     *     List&lt;{@link me.philcali.device.pool.Device}&gt; devices = devicePool.provisionWait(input, 30, TimeUnit.SECONDS);
     *     <br>
     * </code>
     *
     * @param input The provision request in the form of a {@link me.philcali.device.pool.model.ProvisionInput}
     * @param amount The amount of {@link java.util.concurrent.TimeUnit} in unit
     * @param unit The {@link java.util.concurrent.TimeUnit} to simplify timeout
     * @return a {@link java.util.List} of {@link me.philcali.device.pool.Device} objects
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure to provision devices in the time allotted among other reasons
     */
    default List<Device> provisionWait(ProvisionInput input, long amount, TimeUnit unit) throws ProvisioningException {
        final ProvisionOutput output = provision(input);
        final CompletableFuture<ProvisionOutput> finalized = CompletableFuture.supplyAsync(() -> {
            for (;;) {
                final ProvisionOutput updated = describe(output);
                if (updated.status().isTerminal()) {
                    return updated;
                }
            }
        });
        try {
            final ProvisionOutput updated = finalized.get(amount, unit);
            if (!updated.succeeded()) {
                throw new ProvisioningException("Provision " + output.id() + " failed");
            }
            return obtain(updated);
        } catch (TimeoutException | InterruptedException e) {
            throw new ProvisioningException("Provision " + output.id() + " never terminated in time");
        } catch (ExecutionException e) {
            throw new ProvisioningException(e.getCause());
        }
    }

    /** {@inheritDoc} */
    @Override
    default void close() {
        // No-op
    }
}
