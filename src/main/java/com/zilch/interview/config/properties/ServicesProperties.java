package com.zilch.interview.config.properties;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application.config.rest.service")
public record ServicesProperties(
        @Bean @Valid @NestedConfigurationProperty OperationLockServiceProperties operationLock) {
}
