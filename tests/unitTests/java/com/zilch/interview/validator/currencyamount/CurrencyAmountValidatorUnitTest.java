package com.zilch.interview.validator.currencyamount;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyAmountValidatorUnitTest {

    private final CurrencyAmountValidator validator = new CurrencyAmountValidator();

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @Test
    void shouldReturnTrueWhenRequestIsNull() {
        // when
        boolean result = validator.isValid(null, context);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, USD",
            "100.0, GBP",
            "100, USD",
            "100, JPY",
            "100.000, KWD",
            "10.5, EUR"
    })
    void shouldReturnTrueForValidCurrencyAndAmountScale(String amount, String currency) {
        // given
        var request = getPaymentDTORequestBuilder()
                .amount(new BigDecimal(amount))
                .currency(currency)
                .build();

        // when
        boolean result = validator.isValid(request, context);

        // then
        assertThat(result).isTrue();
        verifyNoInteractions(context);
    }

    @Test
    void shouldReturnFalseForUnsupportedCurrencyCode() {
        // given
        var request = getPaymentDTORequestBuilder()
                .amount(new BigDecimal("100.00"))
                .currency("INVALID")
                .build();

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        // when
        boolean result = validator.isValid(request, context);

        // then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Unsupported currency code: INVALID");
    }

    @ParameterizedTest
    @CsvSource({
            "100.123, GBP, 2",
            "100.1, JPY, 0",
            "100.1234, KWD, 3"
    })
    void shouldReturnFalseForInvalidAmountScale(String amount, String currency, int maxAllowed) {
        // given
        var request = getPaymentDTORequestBuilder()
                .amount(new BigDecimal(amount))
                .currency(currency)
                .build();
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        // when
        boolean result = validator.isValid(request, context);

        // then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Invalid amount scale for currency %s. Max allowed: %d".formatted(currency, maxAllowed));
    }

    @Test
    void shouldReturnTrueForAmountWithTrailingZerosThatExceedScaleButCanBeStripped() {
        // given
        var request = getPaymentDTORequestBuilder()
                .amount(new BigDecimal("100.000"))
                .currency("GBP")
                .build();
        // when
        boolean result = validator.isValid(request, context);

        // then
        assertThat(result).isTrue();
    }

    private PaymentRequestDTO.PaymentRequestDTOBuilder getPaymentDTORequestBuilder() {
        return PaymentRequestDTO.builder()
                .userId(UUID.randomUUID())
                .paymentMethod(new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "123456"))
                .merchantId("merchant123")
                .orderId("order123")
                .deviceId("device123");

    }

}
