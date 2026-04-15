package com.zilch.interview.dto;

import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.validator.cardtoken.ValidCardToken;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
public record CardPaymentMethodDTO(@NonNull PaymentMethodType type,
                                   @NonNull @ValidCardToken String cardToken) implements PaymentMethodDTO {
}
