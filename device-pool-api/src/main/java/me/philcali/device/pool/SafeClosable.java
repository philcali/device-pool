/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class SafeClosable {
    private static final Logger LOGGER = LogManager.getLogger(SafeClosable.class);

    private SafeClosable() {
    }

    static void safelyClose(AutoCloseable...closeables) {
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to safely close {}", closeable, e);
            }
        }
    }
}
