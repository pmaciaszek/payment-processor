package com.zilch.interview.model;

public record IdempotencyKey(String key, String requestBodyHash) {
}
