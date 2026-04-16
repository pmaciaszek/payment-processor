package com.zilch.interview.service.check;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.model.CheckResult;
import com.zilch.interview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
class UserCheck implements PaymentRequestCheck {

    private final UserRepository userRepository;

    @Override
    public CheckResult check(PaymentRequestDTO requestDTO) {
        return userRepository.findById(requestDTO.userId())
                .map(this::checkStatus)
                .orElse(CheckResult.fail("User not found"));
    }

    private CheckResult checkStatus(UserEntity userEntity) {
        if (userEntity.getStatus() == UserAccountStatus.ACTIVE) {
            return CheckResult.ok();
        }
        return CheckResult.fail("User is not active");
    }

    @Override
    public CheckStage getCheckStage() {
        return CheckStage.PRE_VALIDATION;
    }
}
