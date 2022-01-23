package me.philcali.device.pool.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@ApiModel
@Value.Immutable
interface LockInputModel {
    String id();

    @Value.Default
    default String holder() {
        try {
            Predicate<InterfaceAddress> localAddress = interfaceAddress -> (
                    interfaceAddress.getAddress().isLinkLocalAddress()
                    || interfaceAddress.getAddress().isAnyLocalAddress()
                    || interfaceAddress.getAddress().isSiteLocalAddress());
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (interfaceEnumeration.hasMoreElements()) {
                NetworkInterface networkInterface = interfaceEnumeration.nextElement();
                if (networkInterface.isUp()) {
                    Optional<String> firstAddress = networkInterface.getInterfaceAddresses().stream()
                            .filter(localAddress.negate())
                            .map(InterfaceAddress::getAddress)
                            .map(InetAddress::getHostAddress)
                            .findFirst();
                    if (firstAddress.isPresent()) {
                        return firstAddress.get();
                    }
                }
            }
            throw new IllegalStateException("Could not find a site to site address for this host");
        } catch (SocketException se) {
            throw new IllegalStateException("Could not find connect to network interface", se);
        }
    }

    @Nullable
    String value();

    @Value.Default
    default long ttl() {
        return TimeUnit.SECONDS.toSeconds(10);
    }
}
