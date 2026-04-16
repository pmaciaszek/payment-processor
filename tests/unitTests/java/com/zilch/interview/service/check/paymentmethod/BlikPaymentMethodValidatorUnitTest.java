package com.zilch.interview.service.check.paymentmethod;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.enums.PaymentMethodType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class BlikPaymentMethodValidatorUnitTest {

    private final BlikPaymentMethodValidator validator = new BlikPaymentMethodValidator();

    @Test
    void shouldBeApplicableForBlik() {
        assertThat(validator.isApplicable(PaymentMethodType.BLIK)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethodType.class, mode = EnumSource.Mode.EXCLUDE, names = "BLIK")
    void shouldNotBeApplicableForCard(PaymentMethodType paymentMethodType) {
        assertThat(validator.isApplicable(paymentMethodType)).isFalse();
    }

    @Test
    void shouldReturnOkForValidBlikCode() {
        // given
        var blikMethod = new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "654321");
        var request = getPaymentDTORequestBuilder().paymentMethod(blikMethod).build();

        // when
        var result = validator.validate(request);

        // then
        assertThat(result.valid()).isTrue();
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"123456"})
    void shouldReturnFailForReservedBlikCode(String code) {
        // given
        var blikMethod = new BlikPaymentMethodDTO(PaymentMethodType.BLIK, code);
        var request = getPaymentDTORequestBuilder().paymentMethod(blikMethod).build();

        // when
        var result = validator.validate(request);

        // then
        assertAll(
                () -> assertThat(result.valid()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo("BLIK code is not active")
        );
    }
}
