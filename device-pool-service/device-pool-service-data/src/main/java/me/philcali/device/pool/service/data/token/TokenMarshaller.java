/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.data.token;

import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.data.exception.TokenMarshallerException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface TokenMarshaller {
    String marshall(CompositeKey owner, Map<String, AttributeValue> lastKey) throws TokenMarshallerException;

    Map<String, AttributeValue> unmarshall(CompositeKey owner, String nextToken) throws TokenMarshallerException;
}
