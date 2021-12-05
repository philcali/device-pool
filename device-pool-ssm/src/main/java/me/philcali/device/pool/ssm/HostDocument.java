package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.Host;

import java.util.function.Function;

@FunctionalInterface
public interface HostDocument extends Function<Host, String> {
    @Override
    String apply(Host host);
}
