/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.data.exception.TokenMarshallerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptedTokenMarshallerTest {
    EncryptedTokenMarshaller source;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        ObjectMapper mapper = new ObjectMapper();
        source = new EncryptedTokenMarshaller(mapper);
    }

    @Test
    void GIVEN_encrypted_marshaller_is_created_WHEN_marshalling_THEN_unmarshalling_is_possible() {
        CompositeKey account = CompositeKey.of("012345678912");
        String nextToken = source.marshall(account, new HashMap<String, AttributeValue>() {{
            put("PK", AttributeValue.builder().s(account.toString()).build());
            put("SK", AttributeValue.builder().s("test").build());
        }});

        Map<String, AttributeValue> lastKey = source.unmarshall(account, nextToken);
        assertNotNull(lastKey);
        assertTrue(lastKey.containsKey("PK"));
        assertEquals(account.toString(), lastKey.get("PK").s());
        assertEquals("test", lastKey.get("SK").s());
    }

    @Test
    void GIVEN_encrypted_marshaller_is_created_WHEN_differing_keys_THEN_decryption_fails() throws Exception {
        CompositeKey account = CompositeKey.of("012345678912");
        String nextToken = source.marshall(account, new HashMap<String, AttributeValue>() {{
            put("PK", AttributeValue.builder().s(account.toString()).build());
            put("SK", AttributeValue.builder().s("test").build());
        }});
        // Fails because invalid secret is used
        assertThrows(InvalidInputException.class, () -> source.unmarshall(CompositeKey.of("account2"), nextToken));
        // Fails because encrypted contents can't be parsed
        String invalidToken = source.encrypt("Hello".getBytes(StandardCharsets.UTF_8), source.generateSecret(account));
        assertThrows(TokenMarshallerException.class, () -> source.unmarshall(account, invalidToken));
        // Fails because decryption is invalid
        assertThrows(InvalidInputException.class, () -> source.unmarshall(account, "Hello"));
    }
}
