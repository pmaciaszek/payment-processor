package com.zilch.interview.service;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.entity.UserTransferEntity;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.repository.UserTransferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferPersistenceService {

    private final UserTransferRepository userTransferRepository;

    @Transactional
    public UserTransferEntity createPendingTransferEntity(PaymentRequestDTO requestDTO) {
        return userTransferRepository.save(UserTransferEntity.ofPendingTransfer(requestDTO));
    }

    @Transactional
    public void updateTransferStatus(UUID id, TransferResponseDTO transferResult) {
        var entity = findEntityById(id);
        entity.setStatus(transferResult.status());
        entity.setTransferId(transferResult.id());
        entity.setStatusDescription(transferResult.reason());
    }

    @Transactional
    public void markAsFailed(UUID id, String message) {
        var entity = findEntityById(id);
        entity.setStatus(TransferStatus.FAILED);
        entity.setStatusDescription(message);
    }

    private UserTransferEntity findEntityById(UUID id) {
        return userTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Transfer entity not found id: " + id));
    }
}
