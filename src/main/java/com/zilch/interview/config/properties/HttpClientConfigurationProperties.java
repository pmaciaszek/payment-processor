package com.zilch.interview.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("application.config.rest")
public record HttpClientConfigurationProperties(
        Duration evictIdleConnectionTimeout,
        Integer maxConnectionsPerRoute,
        Integer maxConnectionsTotal,
        Duration connectionTimeout,
        Duration connectionRequestTimeout,
        Duration socketTimeout) {
}
