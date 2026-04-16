package com.zilch.interview.service;

import com.zilch.interview.model.PaymentResult;
import com.zilch.interview.service.check.PaymentRequestValidatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private PaymentRequestValidatorService validatorService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldProcessPaymentSuccessfully() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();

        // when
        var result = paymentService.processPayment(requestDTO);

        // then
        assertThat(result)
                .returns(true, PaymentResult::success)
                .returns("some-transaction-id", PaymentResult::transactionId);
        verify(validatorService).runChecks(requestDTO);
    }
}
