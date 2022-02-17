/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.local;

import me.philcali.device.pool.service.module.DaggerDevicePoolsComponent;
import me.philcali.device.pool.service.module.DevicePoolsComponent;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * <p>Server class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public final class Server {
    private final HttpServer httpServer;

    public static class Builder {
        private String endpoint;
        private DevicePoolsComponent component;

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder component(DevicePoolsComponent component) {
            this.component = component;
            return this;
        }

        public Server build() {
            Objects.requireNonNull(component, "component is required");
            Objects.requireNonNull(endpoint, "endpoint is required");
            return new Server(this);
        }
    }

    private Server(Builder builder) {
        this.httpServer = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(builder.endpoint),
                builder.component.application(),
                false
        );
    }

    /**
     * <p>builder.</p>
     *
     * @return a {@link me.philcali.device.pool.service.local.Server.Builder} object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>isStarted.</p>
     *
     * @return a boolean
     */
    public boolean isStarted() {
        return httpServer.isStarted();
    }

    /**
     * <p>start.</p>
     *
     * @throws java.io.IOException if any.
     */
    public void start() throws IOException {
        httpServer.start();
    }

    /**
     * <p>stop.</p>
     */
    public void stop() {
        httpServer.shutdownNow();
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects
     * @throws java.io.IOException if any.
     */
    public static void main(String[] args) throws IOException {
        String endpoint = "http://localhost:8080";
        Server server = Server.builder()
                .component(DaggerDevicePoolsComponent.create())
                .endpoint(endpoint)
                .build();

        server.start();
        if (server.isStarted()) {
            System.out.println("Server is reachable on " + endpoint);
        }
    }
}
