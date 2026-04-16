package com.zilch.interview.service.check;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;

public interface PaymentRequestCheck {

    CheckResult check(PaymentRequestDTO requestDTO);

    CheckStage getCheckStage();

}
