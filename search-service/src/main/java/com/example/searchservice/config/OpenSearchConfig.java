package com.example.searchservice.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Configuration
@Profile("local")
public class OpenSearchConfig {

    @Value("${opensearch.local.host}")
    private String host;

    @Value("${opensearch.local.port}")
    private int port;

    @Value("${opensearch.local.scheme}")
    private String scheme;

    @Value("${opensearch.local.username}")
    private String username;

    @Value("${opensearch.local.password}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() {
        try {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

            SSLContextBuilder sslBuilder = SSLContextBuilder.create();
            sslBuilder.loadTrustMaterial(null, TrustAllStrategy.INSTANCE);
            RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                            try {
                                return httpClientBuilder
                                    .setSSLContext(sslBuilder.build())
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                    .setDefaultCredentialsProvider(credentialsProvider);
                            } catch (NoSuchAlgorithmException e) {
                                throw new RuntimeException(e);
                            } catch (KeyManagementException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            RestClient restClient = builder.build();
            OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            return new OpenSearchClient(transport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpenSearchClient with trust-all SSL context and basic auth", e);
        }
    }
}
