package com.zilch.interview.config.properties;

import lombok.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
public record OperationLockServiceProperties(@NonNull Integer maxRetries) {
}
