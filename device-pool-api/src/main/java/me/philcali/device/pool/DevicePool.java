/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.configuration.DevicePoolConfigProperties;
import me.philcali.device.pool.connection.ConnectionFactory;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ProvisioningException;
import me.philcali.device.pool.local.LocalDevicePool;
import me.philcali.device.pool.model.ProvisionInput;
import me.philcali.device.pool.model.ProvisionOutput;
import me.philcali.device.pool.provision.ProvisionService;
import me.philcali.device.pool.reservation.ReservationService;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

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
     * Attempts to create a {@link me.philcali.device.pool.DevicePool} from an input stream, pointing at a properties
     * file. An application might provide a <code>devices/pool.properties</code> file on the classpath. The contents of
     * the file for building a valid {@link me.philcali.device.pool.BaseDevicePool} might look like:
     * <br>
     * <pre>
     * device.pool.class=me.philcali.device.pool.BaseDevicePool
     * device.pool.provision=me.philcali.device.pool.provision.LocalProvisionService
     * device.pool.provision.local.hosts.host1.address=myhost.example.com
     * device.pool.provision.local.hosts.host1.platform=unix:armv7
     * device.pool.connection=me.philcali.device.pool.ssh.ConnectionFactorySSH
     * device.pool.connection.ssh.user=ec2-user
     * </pre>
     * <br>
     * Then the following code would work:
     * <br>
     * <pre>
     *     DevicePool pool = DevicePool.create();
     *     List&lt;Device&gt; devices = pool.provisionSync(ProvisionInput.create(), 5, TimeUnit.SECONDS);
     * </pre>
     *  @param inputStream
     * @return
     */
    static DevicePool create(InputStream inputStream) {
        try {
            DevicePoolConfig config = DevicePoolConfigProperties.load(inputStream);
            if (config.poolClassName().equals(LocalDevicePool.class.getName())) {
                return LocalDevicePool.create();
            } else if (config.poolClassName().equals(BaseDevicePool.class.getName())) {
                BaseDevicePool.Builder builder = BaseDevicePool.builder();
                Map<String, Consumer> functions = new LinkedHashMap<>() {{
                    put("provision", provision -> builder.provisionService((ProvisionService) provision));
                    put("reservation", reservation -> builder.reservationService((ReservationService) reservation));
                    put("connection", connection -> builder.connections((ConnectionFactory) connection));
                    put("transfer", transfer -> builder.transfers((ContentTransferAgentFactory) transfer));
                }};
                Map<String, String> linkedFunctions = Map.of(
                        "provision", "reservation",
                        "reservation", "provision",
                        "connection", "transfer",
                        "transfer", "connection"
                );
                Set<Object> createdComponents = new HashSet<>();
                for (Map.Entry<String, Consumer> entry : functions.entrySet()) {
                    String className = config.get(entry.getKey())
                            .orElseGet(() -> config.get(linkedFunctions.get(entry.getKey()))
                                    .orElseThrow(() -> new ProvisioningException("Entry for device.pool."
                                            + entry.getKey() + " does not exist.")));
                    Class<?> componentClass = Class.forName(className);
                    Object component = createdComponents.stream()
                            .filter(c -> componentClass.isAssignableFrom(c.getClass()))
                            .findFirst()
                            .orElse(null);
                    if (component == null) {
                        Class<?> builderClass = Class.forName(className + "$Builder");
                        Method method = builderClass.getDeclaredMethod("fromConfig", DevicePoolConfig.class);
                        component = method.invoke(builderClass.getConstructor().newInstance(), config);
                        createdComponents.add(component);
                    }
                    entry.getValue().accept(component);
                }
                return builder.build();
            }
            throw new ProvisioningException("Could not create a default " + DevicePool.class.getSimpleName());
        } catch (IOException
                | NullPointerException
                | ClassNotFoundException
                | NoSuchMethodException
                | InvocationTargetException
                | IllegalAccessException
                | InstantiationException ie) {
            throw new ProvisioningException(ie);
        }
    }

    static DevicePool create(ClassLoader loader) {
        return create(loader.getResourceAsStream("devices/pool.properties"));
    }

    static DevicePool create() {
        return create(ClassLoader.getSystemClassLoader());
    }

    /**
     * Convenience method for wrapping client side provision workflow in an async future.
     * The future will loop forever, but can be bounded by client control. Note:
     * <br>
     * <pre>
     * var future = devicePool.provisionAsync(ProvisionInput.create());
     * try {
     *     var devices = future.get(5. TimeUnit.MINUTES);
     * } catch (TimeoutException | ExecutionException e) {
     *     e.printStackTrace();
     * }
     * </pre>
     *
     * @param input the {@link me.philcali.device.pool.model.ProvisionInput} request
     * @return a {@link java.util.concurrent.CompletableFuture} of {@link me.philcali.device.pool.Device}s
     */
    default CompletableFuture<List<Device>> provisionAsync(ProvisionInput input) {
        return CompletableFuture.supplyAsync(() -> {
            ProvisionOutput output = provision(input);
            do {
                output = describe(output);
            } while (!output.status().isTerminal());
            if (output.succeeded()) {
                return obtain(output);
            }
            throw new ProvisioningException("Provision " + output.id() + " failed");
        });
    }

    /**
     * Convenience method to block on any provision request. This method handles the polling of
     * a provision request. Example:
     * <br>
     * <pre>
     *     ProvisionInput input = ProvisionInput.builder().amount(5).build();
     *     List&lt;{@link me.philcali.device.pool.Device}&gt; devices = devicePool.provisionSync(input, 30, TimeUnit.SECONDS);
     *     <br>
     * </pre>
     *
     * @param input The provision request in the form of a {@link me.philcali.device.pool.model.ProvisionInput}
     * @param amount The amount of {@link java.util.concurrent.TimeUnit} in unit
     * @param unit The {@link java.util.concurrent.TimeUnit} to simplify timeout
     * @return a {@link java.util.List} of {@link me.philcali.device.pool.Device} objects
     * @throws me.philcali.device.pool.exceptions.ProvisioningException Failure to provision devices in the time allotted among other reasons
     */
    default List<Device> provisionSync(ProvisionInput input, long amount, TimeUnit unit) throws ProvisioningException {
        try {
            return provisionAsync(input).get(amount, unit);
        } catch (TimeoutException | InterruptedException e) {
            throw new ProvisioningException("Provision " + input.id() + " never terminated in time");
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
