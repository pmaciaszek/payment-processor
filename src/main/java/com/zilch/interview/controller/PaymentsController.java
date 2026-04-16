package com.zilch.interview.controller;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.PaymentResponseDTO;
import com.zilch.interview.service.PaymentOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final PaymentOrchestrator paymentOrchestrator;

    @PostMapping
    public PaymentResponseDTO processPayment(@RequestHeader("X-Request-ID") @NotBlank String requestId,
                                             @Valid @RequestBody PaymentRequestDTO requestDTO) {
        return paymentOrchestrator.processPayment(requestId, requestDTO);
    }
}
