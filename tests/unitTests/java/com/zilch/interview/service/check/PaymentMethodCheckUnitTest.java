package com.zilch.interview.service.check;

import com.zilch.interview.dto.BlikPaymentMethodDTO;
import com.zilch.interview.dto.PaymentMethodDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.enums.PaymentMethodType;
import com.zilch.interview.model.CheckResult;
import com.zilch.interview.service.check.paymentmethod.PaymentMethodValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodCheckUnitTest {

    @Mock
    private PaymentMethodValidator<BlikPaymentMethodDTO> blikValidator;

    @Mock
    private PaymentMethodValidator<PaymentMethodDTO> otherValidator;

    private PaymentMethodCheck paymentMethodCheck;

    @BeforeEach
    void setUp() {
        paymentMethodCheck = new PaymentMethodCheck(List.of(blikValidator, otherValidator));
    }

    @Test
    void shouldReturnOkWhenNoValidatorsAreApplicable() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build(); // Default is BLIK but we can mock validators to not be applicable
        when(blikValidator.isApplicable(any())).thenReturn(false);
        when(otherValidator.isApplicable(any())).thenReturn(false);

        // when
        var result = paymentMethodCheck.check(requestDTO);

        // then
        assertThat(result.valid()).isTrue();
        verify(blikValidator, never()).validate(any());
        verify(otherValidator, never()).validate(any());
    }

    @Test
    void shouldReturnOkWhenApplicableValidatorSucceeds() {
        // given
        var blikMethod = new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "123456");
        var requestDTO = getPaymentDTORequestBuilder().paymentMethod(blikMethod).build();
        
        when(blikValidator.isApplicable(PaymentMethodType.BLIK)).thenReturn(true);
        when(blikValidator.validate(blikMethod)).thenReturn(CheckResult.ok());
        when(otherValidator.isApplicable(PaymentMethodType.BLIK)).thenReturn(false);

        // when
        var result = paymentMethodCheck.check(requestDTO);

        // then
        assertThat(result.valid()).isTrue();
        verify(blikValidator).validate(blikMethod);
    }

    @Test
    void shouldReturnFailWhenApplicableValidatorFails() {
        // given
        var blikMethod = new BlikPaymentMethodDTO(PaymentMethodType.BLIK, "123456");
        var requestDTO = getPaymentDTORequestBuilder().paymentMethod(blikMethod).build();
        var failure = CheckResult.fail("Invalid BLIK");

        when(blikValidator.isApplicable(PaymentMethodType.BLIK)).thenReturn(true);
        when(blikValidator.validate(blikMethod)).thenReturn(failure);

        // when
        var result = paymentMethodCheck.check(requestDTO);

        // then
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Invalid BLIK");
    }

    @Test
    void shouldReturnValidationStage() {
        assertThat(paymentMethodCheck.getCheckStage()).isEqualTo(CheckStage.VALIDATION);
    }
}
