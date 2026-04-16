package com.zilch.interview.service.check;

import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VelocityCheck implements PaymentRequestCheck {

    private final ServicesProperties servicesProperties;

    @Override
    public CheckResult check(PaymentRequestDTO requestDTO) {
        int requests = GlobalRequestCounter.increment(requestDTO.userId());

        if (requests > servicesProperties.velocityCheck().maxRequestCount()) {
            return CheckResult.fail("Too many requests");
        }

        return CheckResult.ok();
    }

    @Override
    public CheckStage getCheckStage() {
        return CheckStage.VALIDATION;
    }
}
