package com.zilch.interview.client;

import com.zilch.interview.dto.transfer.TransferRequestDTO;
import com.zilch.interview.dto.transfer.TransferResponseDTO;
import com.zilch.interview.enums.TransferStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("!prod")
public class MockTransferClient implements TransferClient {

    @Override
    public TransferResponseDTO performTransfer(TransferRequestDTO requestDTO) {
        if (requestDTO.amount().equals(new BigDecimal("9.99"))) {
            return new TransferResponseDTO(UUID.randomUUID().toString(),
                    TransferStatus.FAILED, "Processing error");
        }
        return new TransferResponseDTO(UUID.randomUUID().toString(),
                TransferStatus.CAPTURED, null);
    }
}
