package com.zilch.interview.config.properties;

import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("application.config.rest")
public record HttpClientConfigurationProperties(
        @NonNull Duration evictIdleConnectionTimeout,
        @NonNull Integer maxConnectionsPerRoute,
        @NonNull Integer maxConnectionsTotal,
        @NonNull Duration connectionTimeout,
        @NonNull Duration connectionRequestTimeout,
        @NonNull Duration socketTimeout) {
}
