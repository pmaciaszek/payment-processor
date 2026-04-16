package com.zilch.interview.service.check.paymentmethod;

import com.zilch.interview.dto.PaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.model.CheckResult;

public interface PaymentMethodValidator<T extends PaymentMethodDTO> {

    boolean isApplicable(PaymentMethodType type);

    CheckResult validate(PaymentRequestDTO requestDTO);

    @SuppressWarnings("unchecked")
    default T getPaymentMethod(PaymentRequestDTO requestDTO) {
        return (T) requestDTO.paymentMethod();
    }
}
