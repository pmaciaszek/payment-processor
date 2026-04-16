package com.zilch.interview.dto;

import com.zilch.interview.validator.currencyamount.ValidCurrencyAmount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@ValidCurrencyAmount
public record PaymentRequestDTO(
        @NonNull UUID userId,
        @NonNull @Positive BigDecimal amount,
        @NonNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NonNull @Valid PaymentMethodDTO paymentMethod,
        @NonNull @NotBlank String merchantId,
        @NonNull @NotBlank String orderId,
        @NonNull @NotBlank String deviceId) {
}
