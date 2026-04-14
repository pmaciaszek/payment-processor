package com.zilch.interview.config.properties;

import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
public record OperationLockServiceProperties(@NonNull Integer maxRetries,
                                             @Valid @NestedConfigurationProperty OperationLockServiceCacheProperties cache) {
}
