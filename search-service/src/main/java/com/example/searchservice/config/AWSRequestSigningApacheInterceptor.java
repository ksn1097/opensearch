package com.example.searchservice.config;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.net.URI;

public class AWSRequestSigningApacheInterceptor implements HttpRequestInterceptor {
    private final String service;
    private final Aws4Signer signer;
    private final AwsCredentialsProvider credentialsProvider;
    private final Region region;

    public AWSRequestSigningApacheInterceptor(String service, Aws4Signer signer, AwsCredentialsProvider credentialsProvider, Region region) {
        this.service = service;
        this.signer = signer;
        this.credentialsProvider = credentialsProvider;
        this.region = region;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws IOException {
        // Convert the Apache request to an SdkHttpFullRequest
        final byte[] content;
        SdkHttpFullRequest.Builder sdkRequestBuilder = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.valueOf(request.getRequestLine().getMethod().toUpperCase()))
                .uri(URI.create(request.getRequestLine().getUri()));

        // Copy headers
        for (org.apache.http.Header header : request.getAllHeaders()) {
            sdkRequestBuilder.putHeader(header.getName(), header.getValue());
        }

        // If the request has a body, copy it
        if (request instanceof HttpEntityEnclosingRequest enclosingRequest && enclosingRequest.getEntity() != null) {
            content = IoUtils.toByteArray(enclosingRequest.getEntity().getContent());
            sdkRequestBuilder.contentStreamProvider(() -> new java.io.ByteArrayInputStream(content));
        } else {
            content = new byte[0];
        }

        SdkHttpFullRequest sdkRequest = sdkRequestBuilder.build();

        // Sign the request
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        SdkHttpFullRequest signedRequest = signer.sign(sdkRequest, software.amazon.awssdk.auth.signer.params.Aws4SignerParams.builder()
                .signingName(service)
                .signingRegion(region)
                .awsCredentials(credentials)
                .build());

        // Copy signed headers back to the Apache request
        for (String header : signedRequest.headers().keySet()) {
            request.removeHeaders(header);
            for (String value : signedRequest.headers().get(header)) {
                request.addHeader(header, value);
            }
        }

        // If the request has a body, update the entity with the signed content
        if (request instanceof HttpEntityEnclosingRequest enclosingRequest2 && content.length > 0) {
            enclosingRequest2.setEntity(new ByteArrayEntity(content));
        }
    }
}
