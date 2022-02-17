/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.util;

import me.philcali.device.pool.model.PlatformOS;
import picocli.CommandLine;

/**
 * <p>PlatformOSConverter class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class PlatformOSConverter implements CommandLine.ITypeConverter<PlatformOS> {
    /** {@inheritDoc} */
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
