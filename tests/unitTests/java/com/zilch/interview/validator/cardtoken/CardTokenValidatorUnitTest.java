package com.zilch.interview.validator.cardtoken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardTokenValidatorUnitTest {

    private final CardTokenValidator validator = new CardTokenValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "tok_abcdefghij",
            "tok_1234567890",
            "tok_ABCDEFGHIJ",
            "tok_a1B2c3D4e5F6",
            "tok_verylongtoken12345"})
    void shouldReturnTrueForValidTokens(String token) {
        // when
        var result = validator.isValid(token, null);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tok_abcde",
            "tok_123456789",
            "tk_abcdefghij",
            "token_abcdefghij",
            "tok_abc-def-ghi",
            "tok_abc def ghi",
            "",
            "   "})
    void shouldReturnFalseForInvalidTokens(String token) {
        // when
        var result = validator.isValid(token, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForNullToken() {
        // when
        var result = validator.isValid(null, null);

        // then
        assertThat(result).isFalse();
    }
}
