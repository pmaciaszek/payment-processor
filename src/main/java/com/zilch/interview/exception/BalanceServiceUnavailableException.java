package com.zilch.interview.exception;

public final class BalanceServiceUnavailableException extends RuntimeException {

    public BalanceServiceUnavailableException(String message) {
        super(message);
    }

    public BalanceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
