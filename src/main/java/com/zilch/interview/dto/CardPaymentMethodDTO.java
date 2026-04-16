package com.zilch.interview.dto;

import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.validator.cardtoken.ValidCardToken;
import lombok.NonNull;

public record CardPaymentMethodDTO(@NonNull PaymentMethodType type,
                                   @NonNull @ValidCardToken String cardToken) implements PaymentMethodDTO {
    @Override
    public String attribute() {
        return cardToken;
    }
}
