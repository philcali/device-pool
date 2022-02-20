/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformOSTest {

    @Test
    void GIVEN_platform_WHEN_parsed_THEN_equals_string() {
        String platform = "unix:armv7";
        assertEquals(platform, PlatformOS.fromString(platform).toString());
    }

    @Test
    void GIVEN_platform_WHEN_provided_invalid_string_THEN_throws_exception() {
        String platform = "unix.armv7";
        assertThrows(IllegalArgumentException.class, () -> PlatformOS.fromString(platform));
    }
}
