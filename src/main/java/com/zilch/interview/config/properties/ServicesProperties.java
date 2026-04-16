package com.zilch.interview.config.properties;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application.config.service")
public record ServicesProperties(
        @Valid @NestedConfigurationProperty OperationLockServiceProperties operationLock,
        @Valid @NestedConfigurationProperty VelocityCheckProperties velocityCheck) {
}
