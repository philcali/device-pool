/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.client;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.io.IOException;

/**
 * <p>AwsV4SigningInterceptor class.</p>
 *
 * @author philcali
 * @version $Id: $Id
 */
public class AwsV4SigningInterceptor implements Interceptor {
    private static final String SERVICE_NAME = "execute-api";
    private final AwsCredentialsProvider credentialsProvider;
    private final AwsRegionProvider regionProvider;
    private final Aws4Signer signer;

    /**
     * <p>Constructor for AwsV4SigningInterceptor.</p>
     *
     * @param credentialsProvider a {@link software.amazon.awssdk.auth.credentials.AwsCredentialsProvider} object
     * @param regionProvider a {@link software.amazon.awssdk.regions.providers.AwsRegionProvider} object
     * @param signer a {@link software.amazon.awssdk.auth.signer.Aws4Signer} object
     */
    public AwsV4SigningInterceptor(
            final AwsCredentialsProvider credentialsProvider,
            final AwsRegionProvider regionProvider,
            final Aws4Signer signer) {
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
        this.signer = signer;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link me.philcali.device.pool.service.client.AwsV4SigningInterceptor} object
     */
    public static AwsV4SigningInterceptor create() {
        return new AwsV4SigningInterceptor(
                DefaultCredentialsProvider.create(),
                DefaultAwsRegionProviderChain.builder().build(),
                Aws4Signer.create());
    }

    /** {@inheritDoc} */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        SdkHttpFullRequest beforeSigned = toSdkRequest(request);
        SdkHttpFullRequest afterSigned = signer.sign(beforeSigned, ExecutionAttributes.builder()
                .put(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentials)
                .put(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SERVICE_NAME)
                .put(AwsSignerExecutionAttribute.SIGNING_REGION, regionProvider.getRegion())
                .put(AwsSignerExecutionAttribute.TIME_OFFSET, SdkGlobalTime.getGlobalTimeOffset())
                .build());
        return chain.proceed(toRequest(request, afterSigned));
    }

    private Request toRequest(Request origin, SdkHttpFullRequest request) {
        Headers.Builder headers = new Headers.Builder();
        request.headers().forEach((name, values) -> {
            values.forEach(value -> headers.add(name, value));
        });
        return origin.newBuilder()
                .headers(headers.build())
                .build();
    }

    private SdkHttpFullRequest toSdkRequest(Request request) throws IOException {
        ContentStreamProvider streamProvider = null;
        if (request.body() != null) {
            final Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            streamProvider = buffer::inputStream;
        }
        return SdkHttpFullRequest.builder()
                .uri(request.url().uri())
                .headers(request.headers().toMultimap())
                .method(SdkHttpMethod.valueOf(request.method().toUpperCase()))
                .contentStreamProvider(streamProvider)
                .build();
    }
}
