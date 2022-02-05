package me.philcali.device.pool.example.util;

import me.philcali.device.pool.model.PlatformOS;
import picocli.CommandLine;

public class PlatformOSConverter implements CommandLine.ITypeConverter<PlatformOS> {
    @Override
    public PlatformOS convert(String str) {
        String[] parts = str.split(":");
        if (parts.length < 2) {
            throw new CommandLine.TypeConversionException(
                    "platform / os should be in the form of 'os:arch', but was " + str);
        }
        return PlatformOS.of(parts[0], parts[1]);
    }
}
