package com.zilch.interview.utils;

import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentRequestDTOProvider {

    public static PaymentRequestDTO.PaymentRequestDTOBuilder getPaymentDTORequestBuilder() {
        return PaymentRequestDTO.builder()
                .userId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .currency("GBP")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_abcdefghij"))
                .merchantId("merchant-1")
                .orderId("order-1")
                .deviceId("device-1");
    }
}
