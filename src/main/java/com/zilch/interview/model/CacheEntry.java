package com.zilch.interview.model;

public record CacheEntry(PaymentResult paymentResult, int retryCount) {
}
