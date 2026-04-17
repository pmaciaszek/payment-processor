package com.zilch.interview.service.check;

import com.zilch.interview.client.BalanceClient;
import com.zilch.interview.dto.balance.UserBalanceResponseDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceCheckUnitTest {

    @Mock
    private BalanceClient balanceClient;

    @InjectMocks
    private BalanceCheck balanceCheck;

    @Test
    void shouldReturnOkWhenBalanceIsEnough() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .amount(BigDecimal.valueOf(100))
                .build();
        when(balanceClient.getUserBalance(requestDTO.userId(), "GBP"))
                .thenReturn(new UserBalanceResponseDTO(BigDecimal.valueOf(150)));

        // when
        var result = balanceCheck.check(requestDTO);

        // then
        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldReturnOkWhenBalanceIsExactlyEqual() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .amount(BigDecimal.valueOf(100))
                .build();
        when(balanceClient.getUserBalance(requestDTO.userId(), "GBP"))
                .thenReturn(new UserBalanceResponseDTO(BigDecimal.valueOf(100)));

        // when
        var result = balanceCheck.check(requestDTO);

        // then
        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldReturnFailWhenBalanceIsInsufficient() {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .amount(BigDecimal.valueOf(100.01))
                .build();
        when(balanceClient.getUserBalance(requestDTO.userId(), "GBP"))
                .thenReturn(new UserBalanceResponseDTO(BigDecimal.valueOf(100)));

        // when
        var result = balanceCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("Insufficient funds", CheckResult::reason);
    }

    @Test
    void shouldReturnFailWhenBalanceIsNull() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        when(balanceClient.getUserBalance(requestDTO.userId(), "GBP"))
                .thenReturn(new UserBalanceResponseDTO(null));

        // when
        var result = balanceCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("Unable to get current balance", CheckResult::reason);
    }

    @Test
    void shouldReturnCorrectCheckStage() {
        // when
        var stage = balanceCheck.getCheckStage();

        // then
        assertThat(stage).isEqualTo(CheckStage.VALIDATION);
    }
}
