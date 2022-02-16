/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.local;

import me.philcali.device.pool.Device;
import me.philcali.device.pool.exceptions.DeviceInteractionException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ApiModel
@Value.Immutable
abstract class LocalDeviceModel implements Device {
    private static final Logger LOGGER = LogManager.getLogger(LocalDevice.class);
    private static final int BUFFER = 8192;

    abstract Path baseDirectory();

    @Override
    public abstract String id();

    private void copy(Path source, Path destination, Set<CopyOption> options) throws IOException {
        if (options.contains(CopyOption.RECURSIVE)) {
            Files.walk(source).map(source::relativize).filter(file -> !file.toString().isEmpty()).forEach(file -> {
                try {
                    Path dest = destination.resolve(file);
                    Files.copy(source.resolve(file), dest);
                } catch (IOException e) {
                    throw new DeviceInteractionException(e);
                }
            });
        } else {
            Files.copy(source, destination);
        }
    }

    private byte[] pumpReadStream(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER];
        int read = stream.read(buffer);
        while (read > 0) {
            output.write(buffer, 0, read);
            read = stream.read(buffer);
        }
        if (output.size() == 0) {
            return null;
        }
        return output.toByteArray();
    }

    @Override
    public CommandOutput execute(CommandInput input) throws DeviceInteractionException {
        ProcessBuilder builder = new ProcessBuilder().directory(baseDirectory().toFile());
        List<String> commands = builder.command();
        commands.add(input.line());
        Optional.ofNullable(input.args()).ifPresent(commands::addAll);
        try {
            Process process = builder.start();
            if (Objects.nonNull(input.input())) {
                process.getOutputStream().write(input.input());
                process.getOutputStream().flush();
                process.getOutputStream().close();
            }
            if (!process.waitFor(input.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                throw new DeviceInteractionException("Command failed to complete in time on " + id());
            }
            return CommandOutput.builder()
                    .exitCode(process.exitValue())
                    .originalInput(input)
                    .stdout(pumpReadStream(process.getInputStream()))
                    .stderr(pumpReadStream(process.getErrorStream()))
                    .build();
        } catch (InterruptedException | IOException e) {
            LOGGER.error("Failed to execute command {} on {}", input, id());
            throw new DeviceInteractionException(e);
        }
    }

    @Override
    public void copyTo(CopyInput input) throws DeviceInteractionException {
        try {
            copy(
                    Paths.get(input.source()),
                    baseDirectory().resolve(input.destination()),
                    input.options());
        } catch (IOException e) {
            throw new DeviceInteractionException(e);
        }
    }

    @Override
    public void copyFrom(CopyInput input) throws DeviceInteractionException {
        try {
            copy(
                    baseDirectory().resolve(input.source()),
                    Paths.get(input.destination()),
                    input.options());
        } catch (IOException e) {
            throw new DeviceInteractionException(e);
        }
    }

    @Override
    public void close() {
        Device.super.close();
        try (Stream<Path> stream = Files.walk(baseDirectory())) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new DeviceInteractionException(e);
        }
    }
}
