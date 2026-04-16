package com.zilch.interview.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.utils.ConstraintValidationProvider;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

class CardPaymentMethodDTOUnitTest {

    @Test
    void shouldPassValidationForValidCardPaymentMethodDTO() {
        // given
        var cardDTO = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_abcdefghij");

        // when
        var violations = ConstraintValidationProvider.validate(cardDTO);

        // then
        assertThat(violations).isEmpty();
    }


    @Test
    void shouldFailValidationWhenCardTokenIsInvalid() {
        // given
        var invalidCardPaymentMethodDTO = new CardPaymentMethodDTO(PaymentMethodType.CARD, "invalid_token");

        // when
        var violations = ConstraintValidationProvider.validate(invalidCardPaymentMethodDTO);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Invalid card token");
    }
}
