/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.configuration.ConfigBuilder;
import me.philcali.device.pool.configuration.DevicePoolConfig;
import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.model.Host;
import org.immutables.value.Value;
import software.amazon.awssdk.services.s3.S3Client;

@APIShadowModel
@Value.Immutable
abstract class ContentTransferAgentFactoryS3 implements ContentTransferAgentFactory {
    /**
     * <p>s3.</p>
     *
     * @return a {@link software.amazon.awssdk.services.s3.S3Client} object
     */
    @Value.Default
    public S3Client s3() {
        return S3Client.create();
    }

    abstract String bucketName();

    @Value.Default
    AgentCommand command() {
        return AWSCLIAgentCommand.create();
    }

    public static final class Builder
            extends ImmutableContentTransferAgentFactoryS3.Builder
            implements ConfigBuilder<ContentTransferAgentFactoryS3> {
        @Override
        public ContentTransferAgentFactoryS3 fromConfig(DevicePoolConfig config) {
            return config.namespace("transfer.s3")
                    .flatMap(entry -> entry.get("bucket")
                            .map(this::bucketName)
                            .map(ImmutableContentTransferAgentFactoryS3.Builder::build))
                    .orElseThrow(() -> new ContentTransferException("The s3 transfer needs a bucket property"));
        }
    }

    public static ContentTransferAgentFactoryS3 of(String bucketName) {
        return builder().bucketName(bucketName).build();
    }

    public static ContentTransferAgentFactoryS3.Builder builder() {
        return new Builder();
    }

    /** {@inheritDoc} */
    @Override
    public ContentTransferAgent connect(String id, Connection connection, Host host) throws ContentTransferException {
        return ContentTransferAgentS3.builder()
                .connection(connection)
                .s3(s3())
                .bucketName(bucketName())
                .command(command())
                .prefix(String.format("%s/%s", id, host.deviceId()))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        s3().close();
    }
}
