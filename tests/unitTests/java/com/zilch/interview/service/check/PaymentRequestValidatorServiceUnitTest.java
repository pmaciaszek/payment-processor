package com.zilch.interview.service.check;

import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.exception.ValidationCheckException;
import com.zilch.interview.model.CheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRequestValidatorServiceUnitTest {

    @Mock
    private PaymentRequestCheck preValidationCheck1;

    @Mock
    private PaymentRequestCheck preValidationCheck2;

    @Mock
    private PaymentRequestCheck otherStageCheck;

    @InjectMocks
    private PaymentRequestValidatorService validatorService;

    @BeforeEach
    void setUp() {
        when(preValidationCheck1.getCheckStage()).thenReturn(CheckStage.PRE_VALIDATION);
        when(preValidationCheck2.getCheckStage()).thenReturn(CheckStage.PRE_VALIDATION);
        when(otherStageCheck.getCheckStage()).thenReturn(CheckStage.VALIDATION);

        validatorService = new PaymentRequestValidatorService(
                List.of(preValidationCheck1, preValidationCheck2, otherStageCheck)
        );
    }

    @Test
    void shouldPerformAllChecksWhenTheySucceed() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        when(preValidationCheck1.check(requestDTO)).thenReturn(CheckResult.ok());
        when(preValidationCheck2.check(requestDTO)).thenReturn(CheckResult.ok());
        when(otherStageCheck.check(requestDTO)).thenReturn(CheckResult.ok());

        // when
        validatorService.runChecks(requestDTO);

        // then
        verify(preValidationCheck1).check(requestDTO);
        verify(preValidationCheck2).check(requestDTO);
        verify(otherStageCheck).check(requestDTO);
    }

    @Test
    void shouldThrowExceptionWhenPreValidationCheckFails() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var failureResult = CheckResult.fail("Some error");
        when(preValidationCheck1.check(requestDTO)).thenReturn(failureResult);

        // when & then
        assertThatThrownBy(() -> validatorService.runChecks(requestDTO))
                .isInstanceOf(ValidationCheckException.class)
                .hasMessageContaining("Some error");

        verify(preValidationCheck1).check(requestDTO);
        verify(preValidationCheck2, never()).check(requestDTO);
        verify(otherStageCheck, never()).check(requestDTO);
    }

    @Test
    void shouldThrowExceptionWhenValidationCheckFails() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        when(preValidationCheck1.check(requestDTO)).thenReturn(CheckResult.ok());
        when(preValidationCheck2.check(requestDTO)).thenReturn(CheckResult.ok());
        when(otherStageCheck.check(requestDTO)).thenReturn(CheckResult.fail("Validation error"));

        // when & then
        assertThatThrownBy(() -> validatorService.runChecks(requestDTO))
                .isInstanceOf(ValidationCheckException.class)
                .hasMessage("There were some validation errors");

        verify(preValidationCheck1).check(requestDTO);
        verify(preValidationCheck2).check(requestDTO);
        verify(otherStageCheck).check(requestDTO);
    }

    @Test
    void shouldThrowExceptionWhenMultipleValidationChecksFail() {
        // given
        var validationCheck2 = mock(PaymentRequestCheck.class);
        when(validationCheck2.getCheckStage()).thenReturn(CheckStage.VALIDATION);
        
        validatorService = new PaymentRequestValidatorService(
                List.of(preValidationCheck1, preValidationCheck2, otherStageCheck, validationCheck2)
        );

        var requestDTO = getPaymentDTORequestBuilder().build();
        when(preValidationCheck1.check(requestDTO)).thenReturn(CheckResult.ok());
        when(preValidationCheck2.check(requestDTO)).thenReturn(CheckResult.ok());
        when(otherStageCheck.check(requestDTO)).thenReturn(CheckResult.fail("Error 1"));
        when(validationCheck2.check(requestDTO)).thenReturn(CheckResult.fail("Error 2"));

        // when & then
        assertThatThrownBy(() -> validatorService.runChecks(requestDTO))
                .isInstanceOf(ValidationCheckException.class)
                .hasMessage("There were some validation errors");

        verify(preValidationCheck1).check(requestDTO);
        verify(preValidationCheck2).check(requestDTO);
        verify(otherStageCheck).check(requestDTO);
        verify(validationCheck2).check(requestDTO);
    }
}
