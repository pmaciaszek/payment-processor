package com.zilch.interview.utils.provider;

import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.secure;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentRequestDTOProvider {

    public static PaymentRequestDTO.PaymentRequestDTOBuilder getPaymentDTORequestBuilder() {
        return PaymentRequestDTO.builder()
                .userId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .currency("GBP")
                .paymentMethod(new CardPaymentMethodDTO(PaymentMethodType.CARD, "tok_" + secure().nextAlphanumeric(10)))
                .merchantId(secure().nextAlphanumeric(5))
                .orderId(secure().nextAlphanumeric(6))
                .deviceId(secure().nextAlphanumeric(8));
    }
}
