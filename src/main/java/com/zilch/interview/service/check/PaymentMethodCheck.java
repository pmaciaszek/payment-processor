package com.zilch.interview.service.check;

import com.zilch.interview.dto.PaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import com.zilch.interview.service.check.paymentmethod.PaymentMethodValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class PaymentMethodCheck implements PaymentRequestCheck {

    private final List<PaymentMethodValidator<? extends PaymentMethodDTO>> validators;

    @Override
    public CheckResult check(PaymentRequestDTO requestDTO) {
        return validators.stream()
                .filter(validator -> validator.isApplicable(requestDTO.paymentMethod().type()))
                .map(validator -> validator.validate(requestDTO))
                .filter(result -> !result.valid())
                .findFirst()
                .orElse(CheckResult.ok());
    }

    @Override
    public CheckStage getCheckStage() {
        return CheckStage.VALIDATION;
    }
}
