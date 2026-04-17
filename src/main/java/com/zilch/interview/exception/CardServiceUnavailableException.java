package com.zilch.interview.exception;

public final class CardServiceUnavailableException extends RuntimeException {

    public CardServiceUnavailableException(String message) {
        super(message);
    }

    public CardServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
