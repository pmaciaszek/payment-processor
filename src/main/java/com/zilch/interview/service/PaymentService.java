package com.zilch.interview.service;

import com.zilch.interview.client.TransferClient;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.transfer.TransferRequestDTO;
import com.zilch.interview.model.PaymentResult;
import com.zilch.interview.service.check.PaymentRequestValidatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestValidatorService paymentRequestValidatorService;
    private final TransferClient transferClient;
    private final TransferPersistenceService transferPersistenceService;

    public PaymentResult processPayment(PaymentRequestDTO requestDTO) {
        paymentRequestValidatorService.runChecks(requestDTO);
        return performTransfer(requestDTO);
    }

    private PaymentResult performTransfer(PaymentRequestDTO requestDTO) {
        var pendingTransfer = transferPersistenceService.createPendingTransferEntity(requestDTO);
        try {
            var transferResult = transferClient.performTransfer(TransferRequestDTO.fromPaymentRequest(requestDTO));
            transferPersistenceService.updateTransferStatus(pendingTransfer.getId(), transferResult);
            return new PaymentResult(transferResult.status().isSuccess(), transferResult.id());
        } catch (Exception exception) {
            transferPersistenceService.markAsFailed(pendingTransfer.getId(), exception.getMessage());
            throw exception;
        }
    }
}
