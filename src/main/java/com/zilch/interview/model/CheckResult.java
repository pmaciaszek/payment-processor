package com.zilch.interview.model;

public record CheckResult(boolean valid, String reason) {

    public static CheckResult ok() {
        return new CheckResult(true, null);
    }

    public static CheckResult fail(String reason) {
        return new CheckResult(false, reason);
    }
}
