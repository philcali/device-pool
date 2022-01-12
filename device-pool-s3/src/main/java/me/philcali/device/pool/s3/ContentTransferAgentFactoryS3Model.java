package me.philcali.device.pool.s3;

import me.philcali.device.pool.connection.Connection;
import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.content.ContentTransferAgentFactory;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.ApiModel;
import me.philcali.device.pool.model.Host;
import org.immutables.value.Value;
import software.amazon.awssdk.services.s3.S3Client;

@ApiModel
@Value.Immutable
abstract class ContentTransferAgentFactoryS3Model implements ContentTransferAgentFactory {
    @Value.Default
    public S3Client s3() {
        return S3Client.create();
    }

    abstract String bucketName();

    @Value.Default
    AgentCommand command() {
        return AWSCLIAgentCommand.create();
    }

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

    @Override
    public void close() throws Exception {
        s3().close();
    }
}
