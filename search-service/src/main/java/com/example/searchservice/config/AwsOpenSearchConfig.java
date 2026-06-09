package com.example.searchservice.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.regions.Region;
import java.io.IOException;

@Configuration
@Profile({"aws", "dev"})
public class AwsOpenSearchConfig {

    @Value("${opensearch.aws.host}")
    private String host;
    @Value("${opensearch.aws.region}")
    private String region;

    @Bean
    public OpenSearchClient openSearchClient() {
        Aws4Signer signer = Aws4Signer.create();
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        Region awsRegion = Region.of(region);
        AWSRequestSigningApacheInterceptor interceptor = new AWSRequestSigningApacheInterceptor(
                "es", signer, credentialsProvider, awsRegion
        );
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, 443, "https"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.addInterceptorLast(interceptor)
                );
        RestClient restClient = builder.build();
        OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
