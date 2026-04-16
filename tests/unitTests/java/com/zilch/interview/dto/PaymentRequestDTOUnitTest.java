package com.zilch.interview.dto;

import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.utils.ConstraintValidationProvider;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestDTOUnitTest {

    private static final UUID VALID_USER_ID = UUID.randomUUID();
    private static final String VALID_MERCHANT_ID = "merchant123";
    private static final String VALID_ORDER_ID = "order123";
    private static final String VALID_DEVICE_ID = "device123";
    private static final String VALID_CURRENCY = "USD";
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("100.00");
    private static final PaymentMethodDTO VALID_CARD_METHOD = new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_abcdefghij");

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAmounts")
    void shouldFailValidationForInvalidAmount(BigDecimal amount) {
        // given
        var request = getPaymentDTORequestBuilder()
                .amount(amount)
                .build();

        // when
        var violations = ConstraintValidationProvider.validate(request);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("must be greater than 0");
    }

    private static Stream<Arguments> provideInvalidAmounts() {
        return Stream.of(
                Arguments.of(new BigDecimal("-1.00")),
                Arguments.of(BigDecimal.ZERO)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"US", "USDA", "usd", "123", ""})
    void shouldFailValidationForInvalidCurrencyFormat(String currency) {
        // given
        var request = getPaymentDTORequestBuilder()
                .currency(currency)
                .build();

        // when
        var violations = ConstraintValidationProvider.validate(request);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Unsupported currency code: " + currency);
    }

    @ParameterizedTest
    @MethodSource("providerForShouldThrowNullPointerExceptionForNullRequiredFields")
    void shouldThrowNullPointerExceptionForNullRequiredFields(UUID userId,
                                                              BigDecimal amount,
                                                              String currency,
                                                              PaymentMethodDTO paymentMethod,
                                                              String merchantId,
                                                              String orderId,
                                                              String deviceId,
                                                              String expectedMissingField) {
        assertThatThrownBy(() ->  new PaymentRequestDTO(userId, amount, currency, paymentMethod,
                merchantId, orderId, deviceId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(expectedMissingField);
    }

    private static Stream<Arguments> providerForShouldThrowNullPointerExceptionForNullRequiredFields() {
        return Stream.of(
                Arguments.of(null, VALID_AMOUNT, VALID_CURRENCY, VALID_CARD_METHOD, VALID_MERCHANT_ID, VALID_ORDER_ID, VALID_DEVICE_ID, "userId"),
                Arguments.of(VALID_USER_ID, null, VALID_CURRENCY, VALID_CARD_METHOD, VALID_MERCHANT_ID, VALID_ORDER_ID, VALID_DEVICE_ID, "amount"),
                Arguments.of(VALID_USER_ID, VALID_AMOUNT, null, VALID_CARD_METHOD, VALID_MERCHANT_ID, VALID_ORDER_ID, VALID_DEVICE_ID, "currency"),
                Arguments.of(VALID_USER_ID, VALID_AMOUNT, VALID_CURRENCY, null, VALID_MERCHANT_ID, VALID_ORDER_ID, VALID_DEVICE_ID, "paymentMethod"),
                Arguments.of(VALID_USER_ID, VALID_AMOUNT, VALID_CURRENCY, VALID_CARD_METHOD, null, VALID_ORDER_ID, VALID_DEVICE_ID, "merchantId"),
                Arguments.of(VALID_USER_ID, VALID_AMOUNT, VALID_CURRENCY, VALID_CARD_METHOD, VALID_MERCHANT_ID, null, VALID_DEVICE_ID, "orderId"),
                Arguments.of(VALID_USER_ID, VALID_AMOUNT, VALID_CURRENCY, VALID_CARD_METHOD, VALID_MERCHANT_ID, VALID_ORDER_ID, null, "deviceId"));
    }

    @ParameterizedTest
    @MethodSource("providerForShouldFailValidationForBlankRequiredFields")
    void shouldFailValidationForBlankRequiredFields(String merchantId,
                                                    String orderId,
                                                    String deviceId) {
        // given
        var request = getPaymentDTORequestBuilder()
                .merchantId(merchantId)
                .orderId(orderId)
                .deviceId(deviceId)
                .build();

        // when
        var violations = ConstraintValidationProvider.validate(request);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("must not be blank");
    }

    private static Stream<Arguments> providerForShouldFailValidationForBlankRequiredFields() {
        return Stream.of(
                Arguments.of("", VALID_ORDER_ID, VALID_DEVICE_ID),
                Arguments.of(VALID_MERCHANT_ID, "", VALID_DEVICE_ID),
                Arguments.of(VALID_MERCHANT_ID, VALID_ORDER_ID, ""));
    }


    @ParameterizedTest
    @MethodSource("providerForShouldFailValidationForInvalidPaymentMethod")
    void shouldFailValidationWhenNestedPaymentMethodIsInvalid(PaymentMethodDTO paymentMethod, String expectedErrorMessage) {
        // given
        var request = getPaymentDTORequestBuilder()
                .paymentMethod(paymentMethod)
                .build();

        // when
        var violations = ConstraintValidationProvider.validate(request);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(expectedErrorMessage);
    }

    private static Stream<Arguments> providerForShouldFailValidationForInvalidPaymentMethod() {
        return Stream.of(
                Arguments.of(new CardPaymentMethodDTO(PaymentMethodType.CARD, "invalid_token"), "Invalid card token"),
                Arguments.of(new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "123"), "must match \"\\d{6}\""));
    }

    @Test
    void shouldFailValidationForInvalidAmountScaleForCurrency() {
        // given
        var request = getPaymentDTORequestBuilder()
                .amount(new BigDecimal("100.123"))
                .currency("GBP")
                .build();

        // when
        var violations = ConstraintValidationProvider.validate(request);

        // then
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Invalid amount scale for currency GBP. Max allowed: 2");
    }

    private PaymentRequestDTO.PaymentRequestDTOBuilder getPaymentDTORequestBuilder() {
        return PaymentRequestDTO.builder()
                .userId(UUID.randomUUID())
                .amount(VALID_AMOUNT)
                .currency(VALID_CURRENCY)
                .paymentMethod(VALID_CARD_METHOD)
                .merchantId(VALID_MERCHANT_ID)
                .orderId(VALID_ORDER_ID)
                .deviceId(VALID_DEVICE_ID);

    }
}
