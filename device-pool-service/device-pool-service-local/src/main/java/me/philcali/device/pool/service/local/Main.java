package me.philcali.device.pool.service.local;

import me.philcali.device.pool.service.module.DaggerDevicePoolsComponent;
import me.philcali.device.pool.service.module.DevicePoolsComponent;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        DevicePoolsComponent component = DaggerDevicePoolsComponent.create();

        final String endpointPrefix = "http://localhost:8080";
        final HttpServer httpServer = GrizzlyHttpServerFactory
                .createHttpServer(URI.create(endpointPrefix), component.application());
        if (httpServer.isStarted()) {
            System.out.println("Server is reachable on " + endpointPrefix);
        }
    }
}
