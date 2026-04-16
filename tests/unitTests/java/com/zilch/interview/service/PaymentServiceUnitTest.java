package com.zilch.interview.service;

import com.zilch.interview.client.TransferClient;
import com.zilch.interview.dto.transfer.TransferRequestDTO;
import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.entity.UserTransferEntity;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.exception.ValidationCheckException;
import com.zilch.interview.model.PaymentResult;
import com.zilch.interview.service.check.PaymentRequestValidatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private PaymentRequestValidatorService validatorService;

    @Mock
    private TransferClient transferClient;

    @Mock
    private TransferPersistenceService transferPersistenceService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldProcessPaymentSuccessfully() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var pendingTransfer = mock(UserTransferEntity.class);
        var internalId = UUID.randomUUID();
        when(pendingTransfer.getId()).thenReturn(internalId);

        when(transferPersistenceService.createPendingTransferEntity(requestDTO))
                .thenReturn(pendingTransfer);
        when(transferClient.performTransfer(any(TransferRequestDTO.class)))
                .thenReturn(new TransferResponseDTO("transferId", TransferStatus.CAPTURED, null));

        // when
        var result = paymentService.processPayment(requestDTO);

        // then
        assertThat(result)
                .returns(true, PaymentResult::success)
                .returns("transferId", PaymentResult::transactionId);
        verify(validatorService).runChecks(requestDTO);
        verify(transferPersistenceService).createPendingTransferEntity(requestDTO);
        verify(transferClient).performTransfer(any(TransferRequestDTO.class));
        verify(transferPersistenceService).updateTransferStatus(eq(internalId), any());
        verifyNoMoreInteractions(validatorService, transferClient, transferPersistenceService);
    }

    @Test
    void shouldNotProcessPaymentWhenValidationFails() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        doThrow(ValidationCheckException.empty())
                .when(validatorService).runChecks(requestDTO);

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(requestDTO))
                .isInstanceOf(ValidationCheckException.class)
                .hasMessage("There were some validation errors");

        verify(validatorService).runChecks(requestDTO);
        verifyNoInteractions(transferClient, transferPersistenceService);
    }

    @Test
    void shouldMarkAsFailedWhenTransferClientThrowsException() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var pendingTransfer = mock(UserTransferEntity.class);
        var internalId = UUID.randomUUID();
        when(pendingTransfer.getId()).thenReturn(internalId);

        when(transferPersistenceService.createPendingTransferEntity(requestDTO))
                .thenReturn(pendingTransfer);
        when(transferClient.performTransfer(any(TransferRequestDTO.class)))
                .thenThrow(new RuntimeException("Network error"));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(requestDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Network error");

        verify(validatorService).runChecks(requestDTO);
        verify(transferPersistenceService).createPendingTransferEntity(requestDTO);
        verify(transferClient).performTransfer(any(TransferRequestDTO.class));
        verify(transferPersistenceService).markAsFailed(internalId, "Network error");
    }
}
