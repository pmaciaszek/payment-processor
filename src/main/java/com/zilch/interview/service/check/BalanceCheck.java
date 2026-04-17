package com.zilch.interview.service.check;

import com.zilch.interview.client.BalanceClient;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BalanceCheck implements PaymentRequestCheck {

    private final BalanceClient balanceClient;

    @Override
    public CheckResult check(PaymentRequestDTO requestDTO) {
        var currentBalance = balanceClient.getUserBalance(requestDTO.userId(), requestDTO.currency()).runningBalance();
        if (currentBalance == null) {
            return CheckResult.fail("Unable to get current balance");
        }

        if (currentBalance.compareTo(requestDTO.amount()) < 0) {
            return CheckResult.fail("Insufficient funds");
        }
        return CheckResult.ok();
    }

    @Override
    public CheckStage getCheckStage() {
        return CheckStage.VALIDATION;
    }
}
