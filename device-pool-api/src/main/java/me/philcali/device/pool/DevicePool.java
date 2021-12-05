package me.philcali.device.pool;

import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface DevicePool extends AutoCloseable {
    ProvisionOutput provision(ProvisionInput input) throws ProvisioningException;

    ProvisionOutput describe(String provisionId) throws ProvisioningException;

    void release(String provisionId) throws ProvisioningException;

    List<Device> obtain(ProvisionOutput output) throws ProvisioningException;

    default List<Device> provisionWait(ProvisionInput input, long amount, TimeUnit unit) throws ProvisioningException {
        final ProvisionOutput output = provision(input);
        final CompletableFuture<ProvisionOutput> finalized = CompletableFuture.supplyAsync(() -> {
            for (;;) {
                final ProvisionOutput updated = describe(output.id());
                if (updated.status().isTerminal()) {
                    return updated;
                }
            }
        });
        try {
            final ProvisionOutput updated = finalized.get(amount, unit);
            if (!updated.succeeded()) {
                release(output.id());
                throw new ProvisioningException("Provision " + output.id() + " failed");
            }
            return obtain(updated);
        } catch (TimeoutException | InterruptedException e) {
            release(output.id());
            throw new ProvisioningException("Provision " + output.id() + " never terminated in time");
        } catch (ExecutionException e) {
            throw new ProvisioningException(e.getCause());
        }
    }

    @Override
    default void close() {
        // No-op
    }
}
