package com.zilch.interview.config;

import com.zilch.interview.config.properties.HttpClientConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HttpClientConfiguration {

    private final HttpClientConfigurationProperties clientConfigurationProperties;

    @Bean
    public CloseableHttpClient closeableHttpClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(getConnectionManager())
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.of(clientConfigurationProperties.evictIdleConnectionTimeout()))
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.of(clientConfigurationProperties.connectionRequestTimeout()))
                        .build())
                .build();
    }

    private PoolingHttpClientConnectionManager getConnectionManager() {
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(clientConfigurationProperties.connectionTimeout()))
                        .setSocketTimeout(Timeout.of(clientConfigurationProperties.socketTimeout()))
                        .build())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
                .setMaxConnPerRoute(clientConfigurationProperties.maxConnectionsPerRoute())
                .setMaxConnTotal(clientConfigurationProperties.maxConnectionsTotal())
                .build();
    }
}
