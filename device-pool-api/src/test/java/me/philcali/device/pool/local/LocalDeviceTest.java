/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.local;

import me.philcali.device.pool.Device;
import me.philcali.device.pool.exceptions.DeviceInteractionException;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalDeviceTest implements FileMixin {

    private Device device;
    private Path tempDir;
    private Path testDir;

    @BeforeEach
    void setUp() throws IOException {
        testDir = Files.createTempDirectory("test-dir-");
        tempDir = Files.createTempDirectory("host-1-");
        device = LocalDevice.of(tempDir, "host-1");
    }

    @Override
    public Path baseDirectory() {
        return testDir;
    }

    @AfterEach
    void tearDown() throws IOException {
        device.close();
        cleanUp();
    }

    @Test
    void GIVEN_local_device_is_created_WHEN_execute_is_invoked_THEN_process_is_created() {
        CommandOutput output = device.execute(CommandInput.builder()
                .line("echo")
                .addArgs("Hello", "World")
                .build());
        assertEquals(0, output.exitCode());
        assertEquals("Hello World\n", output.toUTF8String());

        CommandOutput input = device.execute(CommandInput.builder()
                .line("grep").addArgs("-m", "2", "test")
                .input("testing\ntestingTwo".getBytes(StandardCharsets.UTF_8))
                .build());
        assertEquals(0, input.exitCode());
        assertEquals("testing\ntestingTwo\n", input.toUTF8String());

        assertThrows(DeviceInteractionException.class, () -> device.execute(CommandInput.of("read")));
        assertThrows(DeviceInteractionException.class, () -> device.execute(CommandInput.builder()
                .line("grep")
                .addArgs("test")
                .timeout(Duration.ofSeconds(1))
                .build()));
    }

    @Test
    void GIVEN_local_device_is_created_WHEN_copy_to_is_invoked_THEN_files_are_copied() throws IOException {
        Path sub = testDir.resolve("sub");
        Path node = sub.resolve("node");
        Files.createDirectories(node);
        Files.createFile(testDir.resolve("test.txt"));
        Files.createFile(sub.resolve("test.txt"));
        Files.createFile(node.resolve("test.txt"));

        device.copyTo(CopyInput.builder()
                .addOptions(CopyOption.RECURSIVE)
                .source(testDir.toAbsolutePath().toString())
                .destination(".")
                .build());

        List<Path> expected = Files.walk(testDir)
                .sorted()
                .map(testDir::relativize)
                .collect(Collectors.toList());
        assertEquals(expected, Files.walk(tempDir)
                .sorted()
                .map(tempDir::relativize)
                .collect(Collectors.toList()));

        assertThrows(DeviceInteractionException.class, () -> device.copyTo(CopyInput.builder()
                .source(testDir.resolve("nothing").toString())
                .destination("nothing")
                .build()));
    }

    @Test
    void GIVEN_local_device_is_created_WHEN_copy_from_is_invoked_THEN_files_are_copied() throws IOException {
        CopyInput input = CopyInput.builder()
                .source("test.txt")
                .destination(testDir.resolve("test.txt").toString())
                .build();
        assertThrows(DeviceInteractionException.class, () -> device.copyFrom(input));

        Files.writeString(tempDir.resolve("test.txt"), "Hello World", StandardCharsets.UTF_8);

        device.copyFrom(input);

        assertEquals("Hello World", Files.readString(testDir.resolve("test.txt"), StandardCharsets.UTF_8));
    }
}
