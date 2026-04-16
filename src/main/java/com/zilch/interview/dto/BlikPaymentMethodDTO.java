package com.zilch.interview.dto;

import com.zilch.interview.enums.PaymentMethodType;
import jakarta.validation.constraints.Pattern;
import lombok.NonNull;

public record BlikPaymentMethodDTO(@NonNull PaymentMethodType type,
                                   @NonNull @Pattern(regexp = "\\d{6}") String blikCode) implements PaymentMethodDTO {
}
