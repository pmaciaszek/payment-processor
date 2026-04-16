package com.zilch.interview.service.check.paymentmethod;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.model.CheckResult;
import org.springframework.stereotype.Component;

@Component
public class BlikPaymentMethodValidator implements PaymentMethodValidator<BlikPaymentMethodDTO> {

    @Override
    public boolean isApplicable(PaymentMethodType type) {
        return PaymentMethodType.BLIK == type;
    }

    @Override
    public CheckResult validate(PaymentRequestDTO paymentRequestDTO) {
        // mock validation
        var paymentMethod = getPaymentMethod(paymentRequestDTO);
        if (paymentMethod.blikCode().isBlank() || paymentMethod.blikCode().equals("123456")) {
            return CheckResult.fail("BLIK code is not active");
        }
        return CheckResult.ok();
    }
}
