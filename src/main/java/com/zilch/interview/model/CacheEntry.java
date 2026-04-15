package com.zilch.interview.model;

import lombok.Builder;

@Builder
public record CacheEntry(PaymentResult paymentResult, int retryCount, String requestBodyHash) {
}
