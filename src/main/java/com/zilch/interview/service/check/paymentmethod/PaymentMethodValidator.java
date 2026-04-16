package com.zilch.interview.service.check.paymentmethod;

import com.zilch.interview.dto.PaymentMethodDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.model.CheckResult;

public interface PaymentMethodValidator<T extends PaymentMethodDTO> {

    boolean isApplicable(PaymentMethodType type);

    CheckResult validate(T paymentMethod);
}
