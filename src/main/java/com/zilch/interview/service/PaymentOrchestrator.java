package com.zilch.interview.service;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.PaymentResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final OperationLockService operationLockService;
    private final IdempotencyKeyFactoryService idempotencyKeyFactoryService;
    private final PaymentService paymentService;

    public PaymentResponseDTO processPayment(String requestId, PaymentRequestDTO requestDTO) {
        var result = operationLockService.execute(
                idempotencyKeyFactoryService.createIdempotencyKey(requestId, requestDTO),
                () -> paymentService.processPayment(requestDTO));
        return new PaymentResponseDTO(result.success(), result.transactionId());
    }
}
