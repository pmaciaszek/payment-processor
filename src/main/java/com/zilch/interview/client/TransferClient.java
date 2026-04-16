package com.zilch.interview.client;

import com.zilch.interview.dto.transfer.TransferRequestDTO;
import com.zilch.interview.dto.transfer.TransferResponseDTO;

public interface TransferClient {

    TransferResponseDTO performTransfer(TransferRequestDTO requestDTO);
}
