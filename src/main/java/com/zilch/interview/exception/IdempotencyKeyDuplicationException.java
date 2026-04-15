package com.zilch.interview.exception;

public final class IdempotencyKeyDuplicationException extends RuntimeException {
    private IdempotencyKeyDuplicationException(String message) {
        super(message);
    }

    public static IdempotencyKeyDuplicationException ofDuplicateKey(String key) {
        return new IdempotencyKeyDuplicationException("Idempotency key: %s was used with different request".formatted(key));
    }
}
