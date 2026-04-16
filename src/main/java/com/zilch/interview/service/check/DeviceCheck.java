package com.zilch.interview.service.check;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import com.zilch.interview.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(2)
@Component
@RequiredArgsConstructor
class DeviceCheck implements PaymentRequestCheck {

    private final UserDeviceRepository userDeviceRepository;

    @Override
    public CheckResult check(PaymentRequestDTO requestDTO) {
        return userDeviceRepository.findById(UserDeviceId.of(requestDTO))
                .map(this::checkTrusted)
                .orElse(CheckResult.fail("Device not recognized"));
    }

    private CheckResult checkTrusted(UserDeviceEntity userDeviceEntity) {
        if (userDeviceEntity.isTrusted()) {
            return CheckResult.ok();
        }
        return CheckResult.fail("Device is not trusted");
    }

    @Override
    public CheckStage getCheckStage() {
        return CheckStage.PRE_VALIDATION;
    }
}
