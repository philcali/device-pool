/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.example.local;

import me.philcali.device.pool.example.Local;
import me.philcali.device.pool.Device;
import me.philcali.device.pool.DevicePool;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.ProvisionInput;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Copy class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
@CommandLine.Command(
        name = "cp",
        description = "Copies files to and from devices",
        subcommands = {CommandLine.HelpCommand.class}
)
public class Copy implements Runnable {
    @CommandLine.Option(
            names = {"-s", "--source"},
            description = "Local file to copy",
            required = true)
    String source;

    @CommandLine.Option(
            names = {"-d", "--destination"},
            description = "Destination to place file",
            required = true)
    String destination;

    @CommandLine.ParentCommand
    Local local;

    // from https://www.baeldung.com/sha-256-hashing-java
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String sha256(Path filePath) {
        try (InputStream fileStream = Files.newInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] buffer = new byte[1024 * 8];
            int read = fileStream.read(buffer);
            while (read > 0) {
                digest.update(buffer, 0, read);
                read = fileStream.read(buffer);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException ie) {
            throw new IllegalStateException(ie);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        Path localPath = Paths.get(source);
        if (Files.notExists(localPath)) {
            throw new IllegalArgumentException("file " + source + " does not exist");
        }
        DevicePool pool = local.createPool();
        List<Device> devices = pool.provisionSync(ProvisionInput.builder()
                .id("test-send")
                .amount(local.hostNames().size())
                .build(), 10, TimeUnit.SECONDS);
        Path currentPath = Paths.get(".");
        for (Device device : devices) {
            device.copyTo(CopyInput.builder()
                    .source(source)
                    .destination(destination)
                    .build());
            Path copiedFromHost = currentPath.resolve(device.id() + "." + localPath.getFileName());
            device.copyFrom(CopyInput.builder()
                    .source(destination)
                    .destination(copiedFromHost.toString())
                    .build());
            assert sha256(localPath).equals(sha256(copiedFromHost));
            System.out.println("Successfully round trip " + copiedFromHost);
        }
    }
}
