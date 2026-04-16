package com.zilch.interview.service.check;

import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.model.CheckResult;
import com.zilch.interview.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCheckUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCheck userCheck;

    @Test
    void shouldReturnSuccessWhenUserIsActive() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var user = UserEntity.builder()
                .id(requestDTO.userId())
                .status(UserAccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(requestDTO.userId())).thenReturn(Optional.of(user));

        // when
        var result = userCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(true, CheckResult::valid)
                .returns(null, CheckResult::reason);
    }

    @Test
    void shouldReturnFailureWhenUserNotFound() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();

        when(userRepository.findById(requestDTO.userId())).thenReturn(Optional.empty());

        // when
        var result = userCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("User not found", CheckResult::reason);
    }

    @ParameterizedTest
    @EnumSource(value = UserAccountStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
    void shouldReturnFailureWhenUserIsNotActive(UserAccountStatus status) {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var user = UserEntity.builder()
                .id(requestDTO.userId())
                .status(status)
                .build();

        when(userRepository.findById(requestDTO.userId())).thenReturn(Optional.of(user));

        // when
        var result = userCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("User is not active", CheckResult::reason);
    }

    @Test
    void shouldReturnPreValidationStage() {
        // when
        var stage = userCheck.getCheckStage();

        // then
        assertThat(stage).isEqualTo(CheckStage.PRE_VALIDATION);
    }
}
