package com.zilch.interview.service;

import com.zilch.interview.dto.PaymentRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final OperationLockService operationLockService;
    private final IdempotencyKeyFactoryService idempotencyKeyFactoryService;
    private final PaymentService paymentService;

    public void processPayment(String requestId, PaymentRequestDTO requestDTO) {
        operationLockService.execute(
                idempotencyKeyFactoryService.createIdempotencyKey(requestId, requestDTO),
                () -> paymentService.processPayment(requestDTO));
    }
}
