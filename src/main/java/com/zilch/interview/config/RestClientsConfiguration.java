package com.zilch.interview.config;

import com.zilch.interview.client.handler.BalanceClientErrorHandler;
import com.zilch.interview.client.handler.CardClientErrorHandler;
import com.zilch.interview.config.properties.RestClientProperties;
import com.zilch.interview.config.properties.RestClientsProperties;
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

    @Bean
    public RestClient balanceRestClient(RestClientsProperties restClientsProperties,
                                        BalanceClientErrorHandler balanceClientErrorHandler) {
        return restClientBuilder(restClientsProperties.balance(), balanceClientErrorHandler)
                .build();
    }

    @Bean
    public RestClient cardRestClient(RestClientsProperties restClientsProperties,
                                     CardClientErrorHandler cardClientErrorHandler) {
        return restClientBuilder(restClientsProperties.card(), cardClientErrorHandler)
                .build();
    }

    private RestClient.Builder restClientBuilder(RestClientProperties<?> properties, ResponseErrorHandler errorHandler) {
        return RestClient.builder()
                .defaultStatusHandler(errorHandler)
                //.requestInterceptor() outbound logging interceptor should be here
                .requestFactory(getRequestFactory(closeableHttpClient))
                .uriBuilderFactory(new DefaultUriBuilderFactory(properties.getHost()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    private static ClientHttpRequestFactory getRequestFactory(CloseableHttpClient closeableHttpClient) {
        return new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(closeableHttpClient));
    }
}
