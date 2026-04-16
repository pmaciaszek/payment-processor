package com.zilch.interview.service;

import com.zilch.interview.model.IdempotencyKey;
import com.zilch.interview.model.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestratorUnitTest {

    @Mock
    private OperationLockService operationLockService;

    @Mock
    private IdempotencyKeyFactoryService idempotencyKeyFactoryService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentOrchestrator paymentOrchestrator;

    @Test
    void shouldOrchestratePayment() {
        // given
        var requestId = "test-request-id";
        var requestDTO = getPaymentDTORequestBuilder().build();
        var idempotencyKey = new IdempotencyKey(requestId, "hash");
        var expectedResult = new PaymentResult(true, "tx-id");

        when(idempotencyKeyFactoryService.createIdempotencyKey(requestId, requestDTO)).thenReturn(idempotencyKey);
        when(operationLockService.execute(eq(idempotencyKey), any())).thenReturn(expectedResult);

        // when
        paymentOrchestrator.processPayment(requestId, requestDTO);

        // then
        var supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(operationLockService).execute(eq(idempotencyKey), supplierCaptor.capture());
        
        // verify supplier execution
        var capturedSupplier = supplierCaptor.getValue();
        when(paymentService.processPayment(requestDTO)).thenReturn(expectedResult);
        var actualResult = capturedSupplier.get();
        
        assertThat(actualResult).isEqualTo(expectedResult);
        verify(paymentService).processPayment(requestDTO);
    }
}
