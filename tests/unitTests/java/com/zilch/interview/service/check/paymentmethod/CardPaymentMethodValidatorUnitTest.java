package com.zilch.interview.service.check.paymentmethod;

import com.zilch.interview.client.CardClient;
import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.card.CardLimitsResponseDTO;
import com.zilch.interview.dto.card.CardValidationRequestDTO;
import com.zilch.interview.dto.card.CardValidationResponseDTO;
import com.zilch.interview.enums.CardStatus;
import com.zilch.interview.enums.PaymentMethodType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardPaymentMethodValidatorUnitTest {

    @Mock
    private CardClient cardClient;

    @InjectMocks
    private CardPaymentMethodValidator validator;

    @Test
    void shouldBeApplicableForCard() {
        assertThat(validator.isApplicable(PaymentMethodType.CARD)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethodType.class, mode = EnumSource.Mode.EXCLUDE, names = "CARD")
    void shouldNotBeApplicableForBlik(PaymentMethodType paymentMethodType) {
        assertThat(validator.isApplicable(paymentMethodType)).isFalse();
    }

    @Test
    void shouldReturnOkWhenCardIsValidAndWithinLimits() {
        // given
        var cardMethod = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_123");
        var request = getPaymentDTORequestBuilder()
                .paymentMethod(cardMethod)
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .build();

        var limits = new CardLimitsResponseDTO(new BigDecimal("100.00"), "GBP");
        var response = new CardValidationResponseDTO(CardStatus.ACTIVE, limits);

        when(cardClient.validateCard(any(CardValidationRequestDTO.class))).thenReturn(response);

        // when
        var result = validator.validate(request);

        // then
        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldReturnFailWhenCardIsNotActive() {
        // given
        var cardMethod = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_expired");
        var request = getPaymentDTORequestBuilder().paymentMethod(cardMethod).build();

        var limits = new CardLimitsResponseDTO(new BigDecimal("100.00"), "GBP");
        var response = new CardValidationResponseDTO(CardStatus.EXPIRED, limits);

        when(cardClient.validateCard(any(CardValidationRequestDTO.class))).thenReturn(response);

        // when
        var result = validator.validate(request);

        // then
        assertAll(
                () -> assertThat(result.valid()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo(CardStatus.EXPIRED.getValidationMessage())
        );
    }

    @Test
    void shouldReturnFailWhenCurrencyIsNotSupported() {
        // given
        var cardMethod = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_123");
        var request = getPaymentDTORequestBuilder()
                .paymentMethod(cardMethod)
                .currency("EUR")
                .build();

        var limits = new CardLimitsResponseDTO(new BigDecimal("100.00"), "GBP");
        var response = new CardValidationResponseDTO(CardStatus.ACTIVE, limits);

        when(cardClient.validateCard(any(CardValidationRequestDTO.class))).thenReturn(response);

        // when
        var result = validator.validate(request);

        // then
        assertAll(
                () -> assertThat(result.valid()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo("Currency is not supported")
        );
    }

    @Test
    void shouldReturnFailWhenMaxAmountIsNull() {
        // given
        var cardMethod = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_123");
        var request = getPaymentDTORequestBuilder()
                .paymentMethod(cardMethod)
                .currency("GBP")
                .build();

        var limits = new CardLimitsResponseDTO(null, "GBP");
        var response = new CardValidationResponseDTO(CardStatus.ACTIVE, limits);

        when(cardClient.validateCard(any(CardValidationRequestDTO.class))).thenReturn(response);

        // when
        var result = validator.validate(request);

        // then
        assertAll(
                () -> assertThat(result.valid()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo("Currency is not supported")
        );
    }

    @Test
    void shouldReturnFailWhenAmountExceedsLimits() {
        // given
        var cardMethod = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_123");
        var request = getPaymentDTORequestBuilder()
                .paymentMethod(cardMethod)
                .amount(new BigDecimal("150.00"))
                .currency("GBP")
                .build();

        var limits = new CardLimitsResponseDTO(new BigDecimal("100.00"), "GBP");
        var response = new CardValidationResponseDTO(CardStatus.ACTIVE, limits);

        when(cardClient.validateCard(any(CardValidationRequestDTO.class))).thenReturn(response);

        // when
        var result = validator.validate(request);

        // then
        assertAll(
                () -> assertThat(result.valid()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo("Amount exceeds limits")
        );
    }
}
