package com.zilch.interview.service.check;

import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import com.zilch.interview.repository.UserDeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCheckUnitTest {

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private DeviceCheck deviceCheck;

    @Test
    void shouldReturnSuccessWhenDeviceIsTrusted() {
        // given
        var userId = UUID.randomUUID();
        var deviceId = "device-123";
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(userId)
                .deviceId(deviceId)
                .build();
        var id = UserDeviceId.of(requestDTO);
        var device = UserDeviceEntity.builder()
                .id(id)
                .trusted(true)
                .build();

        when(userDeviceRepository.findById(id)).thenReturn(Optional.of(device));

        // when
        var result = deviceCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(true, CheckResult::valid)
                .returns(null, CheckResult::reason);
    }

    @Test
    void shouldReturnFailureWhenDeviceNotFound() {
        // given
        var userId = UUID.randomUUID();
        var deviceId = "device-123";
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(userId)
                .deviceId(deviceId)
                .build();
        var id = UserDeviceId.of(requestDTO);

        when(userDeviceRepository.findById(id)).thenReturn(Optional.empty());

        // when
        var result = deviceCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("Device not recognized", CheckResult::reason);
    }

    @Test
    void shouldReturnFailureWhenDeviceIsNotTrusted() {
        // given
        var userId = UUID.randomUUID();
        var deviceId = "device-123";
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(userId)
                .deviceId(deviceId)
                .build();
        var id = UserDeviceId.of(requestDTO);
        var device = UserDeviceEntity.builder()
                .id(id)
                .trusted(false)
                .build();

        when(userDeviceRepository.findById(id)).thenReturn(Optional.of(device));

        // when
        var result = deviceCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("Device is not trusted", CheckResult::reason);
    }

    @Test
    void shouldReturnPreValidationStage() {
        // when
        var stage = deviceCheck.getCheckStage();

        // then
        assertThat(stage).isEqualTo(CheckStage.PRE_VALIDATION);
    }
}
