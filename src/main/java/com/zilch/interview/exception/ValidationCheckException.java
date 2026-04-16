package com.zilch.interview.exception;

import com.zilch.interview.model.CheckResult;

public final class ValidationCheckException extends RuntimeException {
    private ValidationCheckException(String message) {
        super(message);
    }

    public static ValidationCheckException of(CheckResult checkResult) {
        return new ValidationCheckException(checkResult.reason());
    }
}
