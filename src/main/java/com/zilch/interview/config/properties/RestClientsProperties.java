package com.zilch.interview.config.properties;

import com.zilch.interview.config.properties.dummyDomain.DummyDomainClientProperties;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application.config.rest.clients")
public record RestClientsProperties(
        @Valid @NestedConfigurationProperty DummyDomainClientProperties dummyDomain) {
}
