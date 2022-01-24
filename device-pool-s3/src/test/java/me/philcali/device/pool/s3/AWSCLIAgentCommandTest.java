/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.s3;

import me.philcali.device.pool.model.CommandInput;
import me.philcali.device.pool.model.CopyInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class AWSCLIAgentCommandTest {

    private AgentCommand command;
    private Path temp;

    @BeforeEach
    void setup() throws IOException {
        temp = Files.createTempDirectory("some-prefix");
        command = AWSCLIAgentCommand.create();
    }

    @AfterEach
    void teardown() throws IOException {
        Files.deleteIfExists(temp);
    }

    @Test
    void GIVEN_agent_command_is_initialized_WHEN_copy_THEN_command_input_is_provided() throws IOException {
        CommandInput input = command.copy(CopyInput.builder()
                .source("s3://bucket/some/path/file.txt")
                .destination(temp.toString())
                .build());

        CommandInput expectedInput = CommandInput.builder()
                .line("aws")
                .addArgs("s3", "cp", "s3://bucket/some/path/file.txt", temp.toString())
                .build();

        assertEquals(expectedInput, input);
    }
}
