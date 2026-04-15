package com.zilch.interview.dto;

import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.utils.ConstraintValidationProvider;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class BlikPaymentMethodDTOUnitTest {

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldPassValidationForValidBlikRequest() {
        // given
        var blikMethod = new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "123456");

        // when
        var violations = ConstraintValidationProvider.validate(blikMethod);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "invalid_code"})
    void shouldFailValidationWhenBlikCodeIsInvalid(String code) {
        // given
        var invalidCardPaymentMethodDTO = new BlikPaymentMethodDTO(PaymentMethodType.BLIK, code);

        // when
        var violations = ConstraintValidationProvider.validate(invalidCardPaymentMethodDTO);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("must match \"\\d{6}\"");
    }
}
