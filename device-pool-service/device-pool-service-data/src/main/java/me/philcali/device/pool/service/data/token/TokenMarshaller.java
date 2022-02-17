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

/**
 * <p>TokenMarshaller interface.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public interface TokenMarshaller {
    /**
     * <p>marshall.</p>
     *
     * @param owner a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param lastKey a {@link java.util.Map} object
     * @return a {@link java.lang.String} object
     * @throws me.philcali.device.pool.service.data.exception.TokenMarshallerException if any.
     */
    String marshall(CompositeKey owner, Map<String, AttributeValue> lastKey) throws TokenMarshallerException;

    /**
     * <p>unmarshall.</p>
     *
     * @param owner a {@link me.philcali.device.pool.service.api.model.CompositeKey} object
     * @param nextToken a {@link java.lang.String} object
     * @return a {@link java.util.Map} object
     * @throws me.philcali.device.pool.service.data.exception.TokenMarshallerException if any.
     */
    Map<String, AttributeValue> unmarshall(CompositeKey owner, String nextToken) throws TokenMarshallerException;
}
