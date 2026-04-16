package com.zilch.interview.service;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.model.PaymentResult;
import com.zilch.interview.service.check.PaymentRequestValidatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestValidatorService paymentRequestValidatorService;

    public PaymentResult processPayment(PaymentRequestDTO requestDTO) {
        paymentRequestValidatorService.runChecks(requestDTO);

        return new PaymentResult(true, "some-transaction-id");
    }
}
