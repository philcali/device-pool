/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.philcali.device.pool.service.api.exception.InvalidInputException;
import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.data.exception.TokenMarshallerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EncryptedTokenMarshaller implements TokenMarshaller {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedTokenMarshaller.class);
    private static final int BITS = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String KEY_SPEC = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private final ObjectMapper mapper;
    private final SecureRandom random;

    public EncryptedTokenMarshaller(final ObjectMapper mapper) {
        this.mapper = mapper;
        this.random = new SecureRandom();
    }

    @Nullable
    @Override
    public String marshall(CompositeKey owner, @Nullable Map<String, AttributeValue> lastKey)
            throws TokenMarshallerException {
        if (Objects.isNull(lastKey)) {
            return null;
        }
        try {
            SecretKey secret = generateSecret(owner);
            Map<String, Map<String, String>> converted = lastKey.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<String, String>() {{
                        put("s", entry.getValue().s());
                        put("n", entry.getValue().n());
                    }}));
            return encrypt(mapper.writeValueAsBytes(converted), secret);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Failed to generate secret for {}", owner, e);
            throw new TokenMarshallerException(e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize token for {}: {}", owner, lastKey, e);
            throw new TokenMarshallerException(e);
        } catch (NoSuchPaddingException
                | NoSuchAlgorithmException
                | InvalidKeyException
                | BadPaddingException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException e) {
            LOGGER.error("Failed to marshal nextToken {}", lastKey, e);
            throw new TokenMarshallerException(e);
        }
    }

    @Nullable
    @Override
    public Map<String, AttributeValue> unmarshall(CompositeKey owner, @Nullable String token)
            throws TokenMarshallerException {
        if (Objects.isNull(token)) {
            return null;
        }
        try {
            SecretKey secret = generateSecret(owner);
            Map<String, Map<String, String>> json = mapper.readValue(decrypt(token, secret),
                    new TypeReference<Map<String, Map<String, String>>>() {});
            return json.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> AttributeValue.builder()
                            .s(entry.getValue().get("s"))
                            .n(entry.getValue().get("n"))
                            .build()));
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Failed to generate secret for {}", owner, e);
            throw new TokenMarshallerException(e);
        } catch (BadPaddingException | IllegalArgumentException e) {
            LOGGER.debug("Failed to decrypt, likely due to an invalid key used {}", owner, e);
            throw new InvalidInputException(e);
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | NoSuchPaddingException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException e) {
            LOGGER.error("Failed to unmarshall netToken {}", token, e);
            throw new TokenMarshallerException(e);
        } catch (IOException e) {
            LOGGER.error("Failed to unmarshall {}", owner, e);
            throw new TokenMarshallerException(e);
        }
    }

    protected SecretKey generateSecret(CompositeKey owner) throws InvalidKeySpecException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedBytes = digest.digest(owner.toString().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(encodedBytes, KEY_SPEC);
    }

    protected String encrypt(byte[] input, SecretKey secret)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secret, parameterSpec);
        byte[] cipherText = cipher.doFinal(input);
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    protected byte[] decrypt(String cipherText, SecretKey secret)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(BITS, decodedBytes, 0, GCM_IV_LENGTH);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secret, parameterSpec);
        return cipher.doFinal(decodedBytes, GCM_IV_LENGTH, decodedBytes.length - GCM_IV_LENGTH);
    }
}
