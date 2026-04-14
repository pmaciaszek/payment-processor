package com.zilch.interview.config.properties;

import lombok.NonNull;

import java.time.Duration;

public record OperationLockServiceCacheProperties(@NonNull Integer maxSize, @NonNull Duration ttl) {
}
