package com.zilch.interview.config.properties;

import lombok.NonNull;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
public record OperationLockServiceCacheProperties(@NonNull Integer maxSize, @NonNull Duration ttl) {
}
