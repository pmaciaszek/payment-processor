package com.zilch.interview.config;

import com.zilch.interview.client.handler.DummyDomainClientErrorHandler;
import com.zilch.interview.config.properties.RestClientProperties;
import com.zilch.interview.config.properties.dummyDomain.DummyDomainClientProperties;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
@RequiredArgsConstructor
public class RestClientsConfiguration {

    private final CloseableHttpClient closeableHttpClient;
    private final RestClient.Builder restClientBuilder;

    @Bean
    public RestClient dummpyRestClient(DummyDomainClientProperties dummyDomainClientProperties,
                                       DummyDomainClientErrorHandler dummyDomainClientErrorHandler) {
        return restClientBuilder(dummyDomainClientProperties, dummyDomainClientErrorHandler)
                .build();
    }

    private RestClient.Builder restClientBuilder(RestClientProperties<?> properties, ResponseErrorHandler errorHandler) {
        return restClientBuilder.clone()
                .defaultStatusHandler(errorHandler)
                //.requestInterceptor()
                .requestFactory(getRequestFactory(closeableHttpClient))
                .uriBuilderFactory(new DefaultUriBuilderFactory(properties.getHost()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    private static ClientHttpRequestFactory getRequestFactory(CloseableHttpClient closeableHttpClient) {
        return new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(closeableHttpClient));
    }
}
