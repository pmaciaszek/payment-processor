package com.zilch.interview.service.check.paymentmethod;

import com.zilch.interview.dto.CardPaymentMethodDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.model.CheckResult;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentMethodValidator implements PaymentMethodValidator<CardPaymentMethodDTO> {

    @Override
    public boolean isApplicable(PaymentMethodType type) {
        return PaymentMethodType.CARD == type;
    }

    @Override
    public CheckResult validate(CardPaymentMethodDTO paymentMethod) {
        return CheckResult.ok();
    }
}
