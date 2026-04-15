package com.zilch.interview.controller;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.service.IdempotencyKeyFactoryService;
import com.zilch.interview.service.OperationLockService;
import com.zilch.interview.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final PaymentService paymentService;
    private final OperationLockService operationLockService;
    private final IdempotencyKeyFactoryService idempotencyKeyFactoryService;

    @PostMapping
    public void processPayment(@RequestAttribute String requestId, @Valid @RequestBody PaymentRequestDTO  requestDTO) {
        operationLockService.execute(
                idempotencyKeyFactoryService.createIdempotencyKey(requestId, requestDTO),
                () -> paymentService.processPayment(requestDTO));
    }
}
