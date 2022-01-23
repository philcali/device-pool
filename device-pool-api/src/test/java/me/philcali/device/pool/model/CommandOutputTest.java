/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.model;

import me.philcali.device.pool.exceptions.ConnectionException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class CommandOutputTest {
    @Test
    void GIVEN_command_output_WHEN_converting_output_THEN_returns_bytes() {
        CommandOutput output = CommandOutput.builder()
                .stdout("Hello World".getBytes(StandardCharsets.UTF_8))
                .exitCode(0)
                .build();
        assertArrayEquals("Hello World".getBytes(StandardCharsets.UTF_8), output.toByteArray());
    }

    @Test
    void GIVEN_command_output_WHEN_converting_output_THEN_returns_string() {
        CommandOutput output = CommandOutput.builder()
                .stdout("Hello World".getBytes(StandardCharsets.UTF_8))
                .exitCode(0)
                .build();
        assertEquals("Hello World", output.toUTF8String());
    }

    @Test
    void GIVEN_command_output_WHEN_converting_output_on_error_THEN_throws_exception() {
        CommandOutput output = CommandOutput.builder()
                .stdout("".getBytes(StandardCharsets.UTF_8))
                .stderr("failed".getBytes(StandardCharsets.UTF_8))
                .exitCode(1)
                .build();
        try {
            output.toByteArray();
            fail("Should not get here");
        } catch (ConnectionException e) {
            assertEquals("failed", e.getMessage());
            assertEquals(1, e.errorCode());
        }
    }
}
