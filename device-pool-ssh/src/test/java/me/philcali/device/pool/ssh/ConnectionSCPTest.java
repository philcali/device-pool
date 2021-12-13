package me.philcali.device.pool.ssh;

import me.philcali.device.pool.content.ContentTransferAgent;
import me.philcali.device.pool.exceptions.ContentTransferException;
import me.philcali.device.pool.model.CopyInput;
import me.philcali.device.pool.model.CopyOption;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class ConnectionSCPTest {
    @Mock
    private ScpClient scp;
    @Mock
    private ClientSession session;
    private ContentTransferAgent agent;

    @BeforeEach
    void setup() {
        agent = ConnectionSCP.of(scp, false);
    }

    @Test
    void GIVEN_scp_connection_WHEN_sending_THEN_calls_scp_upload() throws IOException {
        CopyInput input = CopyInput.builder()
                .source("source")
                .destination("destination")
                .addOptions(CopyOption.RECURSIVE)
                .build();
        agent.send(input);
        verify(scp).upload(
                eq(input.source()),
                eq(input.destination()),
                eq(ScpClient.Option.Recursive));
    }

    @Test
    void GIVEN_scp_connection_WHEN_sending_fails_THEN_wrapped_ex_is_thrown() throws IOException {
        CopyInput input = CopyInput.builder()
                .source("source")
                .destination("destination")
                .build();
        doThrow(IOException.class).when(scp).upload(eq(input.source()), eq(input.destination()));
        assertThrows(ContentTransferException.class, () -> agent.send(input));
    }

    @Test
    void GIVEN_scp_connection_WHEN_receiving_fails_THEN_wrapped_ex_is_thrown() throws IOException {
        CopyInput input = CopyInput.builder()
                .source("source")
                .destination("destination")
                .build();
        doThrow(IOException.class).when(scp).download(eq(input.source()), eq(input.destination()));
        assertThrows(ContentTransferException.class, () -> agent.receive(input));
    }

    @Test
    void GIVEN_scp_connection_WHEN_receiving_THEN_calls_scp_download() throws IOException {
        CopyInput input = CopyInput.builder()
                .source("source")
                .destination("destination")
                .build();
        agent.receive(input);
        verify(scp).download(
                eq(input.source()),
                eq(input.destination()));
    }

    @Test
    void GIVEN_scp_connection_WHEN_closing_THEN_only_closes_session_if_reusing() throws Exception {
        doReturn(session).when(scp).getSession();
        agent.close();

        agent = ConnectionSCP.of(scp, true);
        agent.close();

        // called only once
        verify(scp).getSession();
        verify(session).close();
    }
}
