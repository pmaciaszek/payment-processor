package com.zilch.interview.service;

import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.entity.UserTransferEntity;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.repository.UserTransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferPersistenceServiceUnitTest {

    @Mock
    private UserTransferRepository userTransferRepository;

    @InjectMocks
    private TransferPersistenceService transferPersistenceService;

    @Test
    void shouldCreatePendingTransferEntity() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var entity = UserTransferEntity.ofPendingTransfer(requestDTO);
        when(userTransferRepository.save(any(UserTransferEntity.class))).thenReturn(entity);

        // when
        var result = transferPersistenceService.createPendingTransferEntity(requestDTO);

        // then
        assertThat(result).isEqualTo(entity);
        verify(userTransferRepository).save(any(UserTransferEntity.class));
    }

    @Test
    void shouldUpdateTransferStatus() {
        // given
        var id = UUID.randomUUID();
        var entity = new UserTransferEntity();
        var transferResult = new TransferResponseDTO("ext-id", TransferStatus.CAPTURED, "ok");

        when(userTransferRepository.findById(id)).thenReturn(Optional.of(entity));

        // when
        transferPersistenceService.updateTransferStatus(id, transferResult);

        // then
        assertThat(entity.getStatus()).isEqualTo(TransferStatus.CAPTURED);
        assertThat(entity.getTransferId()).isEqualTo("ext-id");
        assertThat(entity.getStatusDescription()).isEqualTo("ok");
        verify(userTransferRepository).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenEntityNotFoundOnUpdate() {
        // given
        var id = UUID.randomUUID();
        var transferResult = new TransferResponseDTO("ext-id", TransferStatus.CAPTURED, "ok");
        when(userTransferRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferPersistenceService.updateTransferStatus(id, transferResult))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transfer entity not found id: " + id);
    }

    @Test
    void shouldMarkAsFailed() {
        // given
        var id = UUID.randomUUID();
        var entity = new UserTransferEntity();
        when(userTransferRepository.findById(id)).thenReturn(Optional.of(entity));

        // when
        transferPersistenceService.markAsFailed(id, "Error message");

        // then
        assertThat(entity.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(entity.getStatusDescription()).isEqualTo("Error message");
        verify(userTransferRepository).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenEntityNotFoundOnMarkAsFailed() {
        // given
        var id = UUID.randomUUID();
        when(userTransferRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferPersistenceService.markAsFailed(id, "Error message"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transfer entity not found id: " + id);
    }
}
