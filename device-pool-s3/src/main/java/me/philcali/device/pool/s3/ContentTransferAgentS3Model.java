/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.exceptions.ConnectionException;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.CommandOutput;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.immutables.value.Value;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

@ApiModel
@Value.Immutable
abstract class ContentTransferAgentS3Model implements ContentTransferAgent {
    private static final Logger LOGGER = LogManager.getLogger(ContentTransferAgentS3.class);

    abstract Connection connection();

    abstract S3Client s3();

    abstract String prefix();

    abstract String bucketName();

    abstract AgentCommand command();

    private String newKey(UUID commandId, Path file) {
        return String.format("%s/%s/%s", prefix(), commandId, file.getFileName());
    }

    @Override
    public void send(CopyInput input) throws ContentTransferException {
        Path contentPath = Paths.get(input.source());
        Consumer<Path> uploadFile = file -> {
            final String newKey = newKey(UUID.randomUUID(), file);
            LOGGER.info("Source location is {} with s3 key {}", file, newKey);
            final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName())
                    .key(newKey)
                    .metadata(new HashMap<String, String>() {{
                        put("source", input.source());
                        put("destination", input.destination());
                    }})
                    .build();
            try {
                PutObjectResponse putObject = s3().putObject(putObjectRequest, RequestBody.fromFile(file));
                LOGGER.info("Uploaded to s3://{}/{}: {}", bucketName(), newKey, putObject.eTag());
                CommandOutput output = connection().execute(command().copy(CopyInput.builder().from(input)
                        .source("s3://" + bucketName() + "/" + newKey)
                        .build()));
                LOGGER.info("Agent copy output: {}", output.toUTF8String());
            } catch (S3Exception | ConnectionException e) {
                throw new ContentTransferException(e);
            }
        };
        if (input.options().contains(CopyOption.RECURSIVE) && Files.isDirectory(contentPath)) {
            try {
                // Could be optimized with an archive
                Files.walk(contentPath)
                        .filter(Files::isRegularFile)
                        .forEach(uploadFile);
            } catch (IOException e) {
                throw new ContentTransferException(e);
            }
        } else {
            uploadFile.accept(contentPath);
        }
    }

    @Override
    public void receive(CopyInput input) throws ContentTransferException {
        final Path targetFile = Paths.get(input.destination());
        final String newKey = newKey(UUID.randomUUID(), targetFile);
        try {
            LOGGER.info("Target location is {} with s3 key {}", targetFile, newKey);
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            final CommandOutput output = connection().execute(command().copy(CopyInput.builder().from(input)
                    .destination("s3://" + bucketName() + "/" + newKey)
                    .build()));
            LOGGER.info("Agent copy output: {}", output.toUTF8String());
            final GetObjectRequest getObject = GetObjectRequest.builder()
                    .bucket(bucketName())
                    .key(newKey)
                    .build();
            try (OutputStream outputStream = Files.newOutputStream(targetFile);
                    ResponseInputStream<GetObjectResponse> object = s3().getObject(getObject)) {
                IoUtils.copy(object, outputStream);
            }
            LOGGER.info("Downloaded from s3://{}/{} to {}", bucketName(), newKey, targetFile);
        } catch (S3Exception | IOException | ConnectionException e) {
            throw new ContentTransferException(e);
        }
    }
}
