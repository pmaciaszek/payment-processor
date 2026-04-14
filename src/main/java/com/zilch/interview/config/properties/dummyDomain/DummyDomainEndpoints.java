package com.zilch.interview.config.properties.dummyDomain;

import jakarta.validation.constraints.NotBlank;

public record DummyDomainEndpoints(@NotBlank String testEndpoint) {
}
