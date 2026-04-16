package com.zilch.interview.service;

import com.zilch.interview.client.TransferClient;
import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.dto.transfer.TransferRequestDTO;
import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.entity.UserTransferEntity;
import com.zilch.interview.model.PaymentResult;
import com.zilch.interview.repository.UserTransferRepository;
import com.zilch.interview.service.check.PaymentRequestValidatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestValidatorService paymentRequestValidatorService;
    private final TransferClient transferClient;
    private final UserTransferRepository userTransferRepository;

    public PaymentResult processPayment(PaymentRequestDTO requestDTO) {
        paymentRequestValidatorService.runChecks(requestDTO);
        return performTransfer(requestDTO);
    }

    private PaymentResult performTransfer(PaymentRequestDTO requestDTO) {
        var transferResult = transferClient.performTransfer(TransferRequestDTO.fromPaymentRequest(requestDTO));
        saveTransferResult(transferResult, requestDTO);
        return new PaymentResult(transferResult.status().isSuccess(), transferResult.id());
    }

    private void saveTransferResult(TransferResponseDTO transferResult, PaymentRequestDTO requestDTO) {
        userTransferRepository.save(UserTransferEntity.fromTransferResult(transferResult, requestDTO));
    }

}
