package com.zilch.interview.dto.transfer;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record TransferRequestDTO(UUID userId,
                                 String merchantId,
                                 String orderId,
                                 BigDecimal amount,
                                 String currency,
                                 PaymentMethodType paymentMethodType,
                                 String paymentMethodAttribute) {

    public static TransferRequestDTO fromPaymentRequest(PaymentRequestDTO requestDTO) {
        return TransferRequestDTO.builder()
                .userId(requestDTO.userId())
                .merchantId(requestDTO.merchantId())
                .orderId(requestDTO.orderId())
                .amount(requestDTO.amount())
                .currency(requestDTO.currency())
                .paymentMethodType(requestDTO.paymentMethod().type())
                .paymentMethodAttribute(requestDTO.paymentMethod().attribute())
                .build();
    }
}
